/*
 * File: BookingsFragment.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Bookings tab. Lists the prosumer's bookings with a search box and Upcoming /
 *              Pending / Approved / Past filters and counts. Search matches station name, slot
 *              day and time, status, kWh or booking reference. Reloads on pull-to-refresh and
 *              whenever the tab is shown, so a booking made from Stations appears straight away. Tapping a card opens its
 *              detail sheet (QR, change time, cancel). With no connection it shows the copy saved
 *              in SQLite under an offline banner.
 */
package com.solargrid.mobile.reservation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.FragmentBookingsBinding
import com.solargrid.mobile.home.ProsumerMainActivity
import com.solargrid.mobile.reservation.models.Reservation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookingsFragment : Fragment(R.layout.fragment_bookings) {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    // Upcoming = live and not over yet; Pending/Approved narrow that down; Past is everything else
    private enum class Filter { UPCOMING, PENDING, APPROVED, PAST }

    // What the empty-state button does
    private enum class Action { NONE, RETRY, FIND_STATION }

    private val reservationManager = ReservationManager.getInstance()

    private var bookings: List<Reservation> = emptyList()
    private var filter = Filter.UPCOMING
    private var loadJob: Job? = null

    // Words typed in the search box; a booking must contain every word
    private var searchWords: List<String> = emptyList()

    // Station id -> name, from the API or the saved copy
    private val stationNames = mutableMapOf<String, String>()

    // Time of the saved copy while showing it offline; null when the list is live
    private var offlineSavedAt: Long? = null

    private val listAdapter = BookingListAdapter(
        stationName = { stationId -> stationId?.let { stationNames[it] } },
        onBookingClick = { booking -> openDetail(booking) }
    )

    // Booking screen in change mode; on success show where the booking moved to
    private val changeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                BookSlotActivity.resultMessage(result.data)?.let { showSnackbar(it) }
                loadBookings(showSpinner = false)
            }
        }

    // Bind views, restore the filter, then load
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookingsBinding.bind(view)

        savedInstanceState?.getString(STATE_FILTER)?.let { name ->
            filter = Filter.entries.firstOrNull { it.name == name } ?: Filter.UPCOMING
        }
        setupList()
        setupFilters()
        setupSearch()
        listenToDetailSheet()
        loadBookings(showSpinner = true)
    }

    // Tabs are hidden, not destroyed; coming back to this tab refreshes the list
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) loadBookings(showSpinner = bookings.isEmpty())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FILTER, filter.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // List, pull-to-refresh and the empty-state button
    private fun setupList() {
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = listAdapter

        binding.swipeBookings.setColorSchemeResources(R.color.text_primary)
        binding.swipeBookings.setOnRefreshListener { loadBookings(showSpinner = false) }
        // Only pull to refresh when the list is at the top
        binding.swipeBookings.setOnChildScrollUpCallback { _, _ ->
            binding.rvBookings.isVisible && binding.rvBookings.canScrollVertically(-1)
        }
    }

    // One chip per filter; the picked one decides what the list shows
    private fun setupFilters() {
        binding.chipGroupFilter.check(chipFor(filter))
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filterFor(checkedIds.firstOrNull())
            showBookings()
        }
        updateCounts()
    }

    // Filter as you type; the keyboard's search key just closes the keyboard.
    // While the first load runs the spinner stays, and the result already uses the words.
    private fun setupSearch() {
        binding.etBookingSearch.doAfterTextChanged { text ->
            searchWords = text?.toString().orEmpty().trim().split(WHITESPACE).filter { it.isNotEmpty() }
            if (loadJob?.isActive != true || bookings.isNotEmpty()) showBookings()
        }
        binding.etBookingSearch.setOnEditorActionListener { field, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            field.clearFocus()
            val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(field.windowToken, 0)
            true
        }
    }

    // Sheet results: "cancelled" reloads the list, "change time" opens the booking screen
    private fun listenToDetailSheet() {
        childFragmentManager.setFragmentResultListener(BookingDetailSheet.REQUEST_BOOKING, viewLifecycleOwner) { _, result ->
            result.getString(BookingDetailSheet.KEY_MESSAGE)?.let { message ->
                showSnackbar(message)
                loadBookings(showSpinner = false)
            }
            result.getString(BookingDetailSheet.KEY_CHANGE_BOOKING)?.let { json ->
                val booking = Gson().fromJson(json, Reservation::class.java)
                BookSlotActivity.newChangeIntent(requireContext(), booking)?.let { changeLauncher.launch(it) }
            }
        }
    }

    private fun openDetail(booking: Reservation) {
        if (childFragmentManager.findFragmentByTag(BookingDetailSheet.TAG) != null) return
        val name = booking.stationId?.let { stationNames[it] }
        BookingDetailSheet.newInstance(booking, name, offline = offlineSavedAt != null)
            .show(childFragmentManager, BookingDetailSheet.TAG)
    }

    // Above the bottom navigation, not on top of it
    private fun showSnackbar(message: String) {
        val view = _binding?.root ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .apply { activity?.findViewById<View>(R.id.bottomNav)?.let { anchorView = it } }
            .show()
    }

    // Fetch bookings and the names of their stations; keep showing the old list if it fails
    private fun loadBookings(showSpinner: Boolean) {
        loadJob?.cancel()
        if (showSpinner && !binding.swipeBookings.isRefreshing) {
            binding.progressBookings.isVisible = true
            binding.rvBookings.isVisible = false
            binding.layoutBookingsEmpty.isVisible = false
        }

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            reservationManager.getMyBookings()
                .onSuccess { result ->
                    stationNames.putAll(result.stationNames)
                    bookings = result.bookings
                    offlineSavedAt = result.savedAt.takeIf { result.offline }
                    showOfflineBanner()
                    showBookings()
                }
                .onFailure { error ->
                    if (bookings.isEmpty()) {
                        showMessage(error.message ?: getString(R.string.booking_error_list), Action.RETRY)
                    } else {
                        showSnackbar(error.message ?: getString(R.string.booking_error_list))
                    }
                }
            binding.progressBookings.isVisible = false
            binding.swipeBookings.isRefreshing = false
        }
    }

    // "You're offline. Showing bookings saved Mon, 28 Sep · 14:32." while on the saved copy
    private fun showOfflineBanner() {
        val savedAt = offlineSavedAt
        binding.tvOfflineBanner.isVisible = savedAt != null
        if (savedAt != null) {
            binding.tvOfflineBanner.text = getString(
                R.string.bookings_offline, ReservationTime.dayLabel(savedAt), ReservationTime.clock(savedAt)
            )
        }
    }

    // Counts on the chips, then the list for the picked filter
    private fun showBookings() {
        updateCounts()
        binding.progressBookings.isVisible = false

        if (bookings.isEmpty()) {
            showMessage(getString(R.string.bookings_empty), Action.FIND_STATION)
            return
        }

        val visible = bookings.filter { matches(it, filter) }
            .let { if (filter == Filter.PAST) it.asReversed() else it }
        if (visible.isEmpty()) {
            val message = if (searchWords.isEmpty()) {
                getString(emptyTextFor(filter))
            } else {
                getString(R.string.bookings_search_empty, searchWords.joinToString(" "))
            }
            showMessage(message, Action.NONE)
            return
        }

        binding.layoutBookingsEmpty.isVisible = false
        binding.rvBookings.isVisible = true
        listAdapter.submit(visible)
    }

    // "Upcoming (2)" and so on
    private fun updateCounts() {
        binding.chipUpcoming.text = getString(R.string.bookings_filter_upcoming, count(Filter.UPCOMING))
        binding.chipPending.text = getString(R.string.bookings_filter_pending, count(Filter.PENDING))
        binding.chipApproved.text = getString(R.string.bookings_filter_approved, count(Filter.APPROVED))
        binding.chipPast.text = getString(R.string.bookings_filter_past, count(Filter.PAST))
    }

    private fun count(filter: Filter): Int = bookings.count { matches(it, filter) }

    // In the chip's group and, when searching, containing every typed word
    private fun matches(booking: Reservation, filter: Filter): Boolean {
        val upcoming = booking.isLive() && !ReservationFormatter.isPast(booking)
        val inFilter = when (filter) {
            Filter.UPCOMING -> upcoming
            Filter.PENDING -> upcoming && booking.status == Reservation.STATUS_PENDING
            Filter.APPROVED -> upcoming && booking.status == Reservation.STATUS_APPROVED
            Filter.PAST -> !upcoming
        }
        return inFilter && matchesSearch(booking)
    }

    // Looks at the same text the card shows, plus the booking reference, ignoring case
    private fun matchesSearch(booking: Reservation): Boolean {
        if (searchWords.isEmpty()) return true
        val context = requireContext()
        val text = listOfNotNull(
            booking.stationId?.let { stationNames[it] },
            ReservationFormatter.timeRange(context, booking),
            getString(ReservationFormatter.statusLabel(booking)),
            ReservationFormatter.energy(context, booking),
            booking.id
        ).joinToString(" ")
        return searchWords.all { text.contains(it, ignoreCase = true) }
    }

    private fun showMessage(message: String, action: Action) {
        binding.rvBookings.isVisible = false
        binding.progressBookings.isVisible = false
        binding.layoutBookingsEmpty.isVisible = true
        binding.tvBookingsEmpty.text = message

        binding.btnBookingsAction.isVisible = action != Action.NONE
        when (action) {
            Action.RETRY -> {
                binding.btnBookingsAction.setText(R.string.bookings_retry)
                binding.btnBookingsAction.setOnClickListener { loadBookings(showSpinner = true) }
            }
            Action.FIND_STATION -> {
                binding.btnBookingsAction.setText(R.string.bookings_find_station)
                binding.btnBookingsAction.setOnClickListener {
                    (activity as? ProsumerMainActivity)?.openTab(R.id.nav_stations)
                }
            }
            Action.NONE -> binding.btnBookingsAction.setOnClickListener(null)
        }
    }

    @StringRes
    private fun emptyTextFor(filter: Filter): Int = when (filter) {
        Filter.UPCOMING -> R.string.bookings_empty_upcoming
        Filter.PENDING -> R.string.bookings_empty_pending
        Filter.APPROVED -> R.string.bookings_empty_approved
        Filter.PAST -> R.string.bookings_empty_past
    }

    private fun chipFor(filter: Filter): Int = when (filter) {
        Filter.UPCOMING -> R.id.chipUpcoming
        Filter.PENDING -> R.id.chipPending
        Filter.APPROVED -> R.id.chipApproved
        Filter.PAST -> R.id.chipPast
    }

    private fun filterFor(chipId: Int?): Filter = when (chipId) {
        R.id.chipPending -> Filter.PENDING
        R.id.chipApproved -> Filter.APPROVED
        R.id.chipPast -> Filter.PAST
        else -> Filter.UPCOMING
    }

    companion object {
        private const val STATE_FILTER = "bookings_filter"
        private val WHITESPACE = Regex("\\s+")
    }
}
