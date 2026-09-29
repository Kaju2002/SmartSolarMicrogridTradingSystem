/*
 * File: DashboardActivity.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Prosumer dashboard. Shows booking counts (summary), bookings filtered by
 *              status (search) and completed transfers (history) from the dashboard API.
 *              Tapping a booking opens the same detail sheet as the Bookings tab.
 */
package com.solargrid.mobile.verification.dashboard

import android.app.Activity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityDashboardBinding
import com.solargrid.mobile.reservation.BookSlotActivity
import com.solargrid.mobile.reservation.BookingDetailSheet
import com.solargrid.mobile.reservation.BookingListAdapter
import com.solargrid.mobile.reservation.models.Reservation
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val dashboardManager = DashboardManager.getInstance()

    // Status sent to the search endpoint; null means every booking
    private var status: String? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    // Station id -> name for the booking cards
    private val stationNames = mutableMapOf<String, String>()

    private val searchAdapter = BookingListAdapter(
        stationName = { stationId -> stationId?.let { stationNames[it] } },
        onBookingClick = { booking -> openDetail(booking) }
    )
    private val historyAdapter = BookingListAdapter(
        stationName = { stationId -> stationId?.let { stationNames[it] } },
        onBookingClick = { booking -> openDetail(booking) }
    )

    // Booking screen in change mode; on success show the message and reload
    private val changeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                BookSlotActivity.resultMessage(result.data)?.let { showSnackbar(it) }
                loadAll()
            }
        }

    // Build the screen, restore the chosen status, then load everything
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        status = savedInstanceState?.getString(STATE_STATUS)
        applyWindowInsets()
        setupTiles()
        setupLists()
        setupChips()
        listenToDetailSheet()
        loadAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_STATUS, status)
    }

    // Keep the top bar below the status bar and the list above the navigation bar
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    // Tapping a count picks that status below; Completed scrolls to the history
    private fun setupTiles() {
        DashboardTiles.setup(binding.tilePending, binding.tileApproved, binding.tileCompleted)
        binding.tilePending.root.setOnClickListener { binding.chipPending.isChecked = true }
        binding.tileApproved.root.setOnClickListener { binding.chipApproved.isChecked = true }
        binding.tileCompleted.root.setOnClickListener {
            binding.scrollDashboard.smoothScrollTo(0, binding.tvHistoryLabel.top)
        }
    }

    // Two lists inside one scroll view, plus back and pull-to-refresh
    private fun setupLists() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rvSearch.layoutManager = LinearLayoutManager(this)
        binding.rvSearch.adapter = searchAdapter
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter

        binding.swipeDashboard.setColorSchemeResources(R.color.text_primary)
        binding.swipeDashboard.setOnRefreshListener { loadAll() }
    }

    // Chips map to the status text the search endpoint expects
    private fun setupChips() {
        chipFor(status)?.let { binding.chipGroupStatus.check(it) }
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            status = statusFor(checkedIds.firstOrNull())
            loadSearch()
        }
    }

    private fun statusFor(chipId: Int?): String? = when (chipId) {
        R.id.chipPending -> Reservation.STATUS_PENDING
        R.id.chipApproved -> Reservation.STATUS_APPROVED
        R.id.chipCancelled -> Reservation.STATUS_CANCELLED
        else -> null
    }

    private fun chipFor(status: String?): Int? = when (status) {
        Reservation.STATUS_PENDING -> R.id.chipPending
        Reservation.STATUS_APPROVED -> R.id.chipApproved
        Reservation.STATUS_CANCELLED -> R.id.chipCancelled
        else -> null
    }

    // Cancel from the sheet reloads; "Change time" opens the booking screen
    private fun listenToDetailSheet() {
        supportFragmentManager.setFragmentResultListener(BookingDetailSheet.REQUEST_BOOKING, this) { _, result ->
            result.getString(BookingDetailSheet.KEY_MESSAGE)?.let { message ->
                showSnackbar(message)
                loadAll()
            }
            result.getString(BookingDetailSheet.KEY_CHANGE_BOOKING)?.let { json ->
                val booking = Gson().fromJson(json, Reservation::class.java)
                BookSlotActivity.newChangeIntent(this, booking)?.let { changeLauncher.launch(it) }
            }
        }
    }

    // Summary, history and search run together; one error line covers any that failed
    private fun loadAll() {
        loadJob?.cancel()
        searchJob?.cancel()
        loadJob = lifecycleScope.launch {
            showSearchLoading(!binding.swipeDashboard.isRefreshing)
            binding.tvDashboardError.isVisible = false

            val requested = status
            val summaryCall = async { dashboardManager.getSummary() }
            val historyCall = async { dashboardManager.getHistory() }
            val searchCall = async { dashboardManager.searchByStatus(requested) }
            val summary = summaryCall.await()
            val history = historyCall.await()
            val search = searchCall.await()

            val bookings = history.getOrDefault(emptyList()) + search.getOrDefault(emptyList())
            stationNames.putAll(dashboardManager.stationNamesFor(bookings))

            summary.onSuccess { DashboardTiles.show(binding.tilePending, binding.tileApproved, binding.tileCompleted, it) }
            showHistory(history)
            // A chip tapped mid-load has started its own search; don't overwrite it
            if (requested == status) {
                showSearch(search)
                showSearchLoading(false)
            }
            listOf(summary, history, search).firstNotNullOfOrNull { it.exceptionOrNull() }
                ?.let { showError(it.message) }

            binding.swipeDashboard.isRefreshing = false
        }
    }

    // Only the status list reloads when a chip changes
    private fun loadSearch() {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            showSearchLoading(true)
            val search = dashboardManager.searchByStatus(status)
            stationNames.putAll(dashboardManager.stationNamesFor(search.getOrDefault(emptyList())))
            showSearch(search)
            search.exceptionOrNull()?.let { showError(it.message) }
            showSearchLoading(false)
        }
    }

    // Results for the chosen status, or a short note when there are none
    private fun showSearch(result: Result<List<Reservation>>) {
        val bookings = result.getOrDefault(emptyList())
        searchAdapter.submit(bookings)
        binding.tvSearchEmpty.isVisible = result.isSuccess && bookings.isEmpty()
    }

    // Completed transfers, newest first
    private fun showHistory(result: Result<List<Reservation>>) {
        val bookings = result.getOrDefault(emptyList())
        historyAdapter.submit(bookings)
        binding.tvHistoryEmpty.isVisible = result.isSuccess && bookings.isEmpty()
    }

    // Spinner in place of the status list while it loads
    private fun showSearchLoading(loading: Boolean) {
        binding.progressSearch.isVisible = loading
        binding.rvSearch.isInvisible = loading
        if (loading) binding.tvSearchEmpty.isVisible = false
    }

    private fun showError(message: String?) {
        val text = message ?: getString(R.string.dashboard_error_load)
        binding.tvDashboardError.text = getString(R.string.dashboard_error_retry, text)
        binding.tvDashboardError.isVisible = true
    }

    // Same sheet as the Bookings tab: QR for approved bookings, change/cancel while allowed
    private fun openDetail(booking: Reservation) {
        if (supportFragmentManager.findFragmentByTag(BookingDetailSheet.TAG) != null) return
        val name = booking.stationId?.let { stationNames[it] }
        BookingDetailSheet.newInstance(booking, name, offline = false)
            .show(supportFragmentManager, BookingDetailSheet.TAG)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val STATE_STATUS = "state_status"
    }
}
