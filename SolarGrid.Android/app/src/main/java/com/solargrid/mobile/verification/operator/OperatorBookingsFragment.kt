/*
 * File: OperatorBookingsFragment.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator Bookings tab. Bookings at the operator's stations with
 *              Pending / Approved / Completed filters and counts. Tapping a card opens its
 *              detail sheet, where Pending bookings can be approved. Reloads on pull-to-refresh,
 *              whenever the tab is shown and after an approval.
 */
package com.solargrid.mobile.verification.operator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.FragmentOperatorBookingsBinding
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OperatorBookingsFragment : Fragment() {

    // Pending = can still be approved; Approved = not over yet; Completed = all transfers
    enum class Filter { PENDING, APPROVED, COMPLETED }

    private var _binding: FragmentOperatorBookingsBinding? = null
    private val binding get() = _binding!!

    private var bookings: List<OperatorBooking> = emptyList()
    private var hasData = false
    private var filter = Filter.PENDING
    private var loadJob: Job? = null

    private val listAdapter = OperatorBookingAdapter { booking -> openDetail(booking) }

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Restore the filter, set up the list and chips, then load
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getString(STATE_FILTER)?.let { name ->
            filter = Filter.entries.firstOrNull { it.name == name } ?: Filter.PENDING
        }
        setupList()
        setupFilters()
        listenToDetailSheet()
        load(showSpinner = true)
    }

    // Tabs are hidden, not destroyed; coming back refreshes the list
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) load(showSpinner = !hasData)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FILTER, filter.name)
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Home tiles open this tab on a matching filter
    fun selectFilter(newFilter: Filter) {
        filter = newFilter
        _binding?.chipGroupOperatorFilter?.check(chipFor(newFilter))
    }

    // List, pull-to-refresh and the retry button
    private fun setupList() {
        binding.rvOperatorBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOperatorBookings.adapter = listAdapter

        binding.swipeOperatorBookings.setColorSchemeResources(R.color.accent_gold)
        binding.swipeOperatorBookings.setOnRefreshListener { load(showSpinner = false) }
        // Only pull to refresh when the list is at the top
        binding.swipeOperatorBookings.setOnChildScrollUpCallback { _, _ ->
            binding.rvOperatorBookings.isVisible && binding.rvOperatorBookings.canScrollVertically(-1)
        }
        binding.btnOperatorBookingsRetry.setOnClickListener { load(showSpinner = true) }
    }

    // One chip per filter; the picked one decides what the list shows
    private fun setupFilters() {
        binding.chipGroupOperatorFilter.check(chipFor(filter))
        binding.chipGroupOperatorFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = filterFor(checkedIds.firstOrNull())
            // Before the first load the error or spinner stays; the list follows once data arrives
            if (hasData) showBookings()
            binding.rvOperatorBookings.scrollToPosition(0)
        }
        updateCounts()
    }

    // After an approval, say so and reload so the booking moves to Approved
    private fun listenToDetailSheet() {
        childFragmentManager.setFragmentResultListener(OperatorBookingSheet.REQUEST_APPROVED, viewLifecycleOwner) { _, _ ->
            showSnackbar(getString(R.string.operator_approved_done))
            load(showSpinner = false)
        }
    }

    private fun openDetail(booking: OperatorBooking) {
        if (childFragmentManager.findFragmentByTag(OperatorBookingSheet.TAG) != null) return
        OperatorBookingSheet.newInstance(booking).show(childFragmentManager, OperatorBookingSheet.TAG)
    }

    // Above the bottom navigation, not on top of it
    private fun showSnackbar(message: String) {
        val view = _binding?.root ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .apply { activity?.findViewById<View>(R.id.bottomNav)?.let { anchorView = it } }
            .show()
    }

    // Fetch the bookings; keep showing the old list if it fails
    private fun load(showSpinner: Boolean) {
        loadJob?.cancel()
        if (showSpinner && !binding.swipeOperatorBookings.isRefreshing) {
            binding.progressOperatorBookings.isVisible = true
            binding.rvOperatorBookings.isVisible = false
            binding.layoutOperatorBookingsEmpty.isVisible = false
        }

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            OperatorManager.getInstance().getBookings()
                .onSuccess { result ->
                    bookings = result
                    hasData = true
                    showBookings()
                }
                .onFailure { error ->
                    val message = error.message ?: getString(R.string.operator_error_bookings)
                    if (hasData) showSnackbar(message) else showMessage(message, retry = true)
                }
            binding.progressOperatorBookings.isVisible = false
            binding.swipeOperatorBookings.isRefreshing = false
        }
    }

    // Counts on the chips, then the list for the picked filter
    private fun showBookings() {
        updateCounts()
        binding.progressOperatorBookings.isVisible = false

        val visible = bookingsFor(filter)
        if (visible.isEmpty()) {
            showMessage(getString(emptyTextFor(filter)), retry = false)
            return
        }
        binding.layoutOperatorBookingsEmpty.isVisible = false
        binding.rvOperatorBookings.isVisible = true
        listAdapter.submit(visible)
    }

    // "Pending (2)" and so on
    private fun updateCounts() {
        binding.chipOperatorPending.text =
            getString(R.string.operator_filter_pending, bookingsFor(Filter.PENDING).size)
        binding.chipOperatorApproved.text =
            getString(R.string.operator_filter_approved, bookingsFor(Filter.APPROVED).size)
        binding.chipOperatorCompleted.text =
            getString(R.string.operator_filter_completed, bookingsFor(Filter.COMPLETED).size)
    }

    private fun bookingsFor(filter: Filter): List<OperatorBooking> = when (filter) {
        Filter.PENDING -> OperatorBookingFormatter.pending(bookings)
        Filter.APPROVED -> OperatorBookingFormatter.approvedUpcoming(bookings)
        Filter.COMPLETED -> OperatorBookingFormatter.completed(bookings)
    }

    // Empty filter or loading error; retry only for errors
    private fun showMessage(message: String, retry: Boolean) {
        binding.rvOperatorBookings.isVisible = false
        binding.progressOperatorBookings.isVisible = false
        binding.layoutOperatorBookingsEmpty.isVisible = true
        binding.tvOperatorBookingsEmpty.text = message
        binding.btnOperatorBookingsRetry.isVisible = retry
    }

    @StringRes
    private fun emptyTextFor(filter: Filter): Int = when (filter) {
        Filter.PENDING -> R.string.operator_bookings_empty_pending
        Filter.APPROVED -> R.string.operator_bookings_empty_approved
        Filter.COMPLETED -> R.string.operator_bookings_empty_completed
    }

    private fun chipFor(filter: Filter): Int = when (filter) {
        Filter.PENDING -> R.id.chipOperatorPending
        Filter.APPROVED -> R.id.chipOperatorApproved
        Filter.COMPLETED -> R.id.chipOperatorCompleted
    }

    private fun filterFor(chipId: Int?): Filter = when (chipId) {
        R.id.chipOperatorApproved -> Filter.APPROVED
        R.id.chipOperatorCompleted -> Filter.COMPLETED
        else -> Filter.PENDING
    }

    companion object {
        private const val STATE_FILTER = "operator_bookings_filter"
    }
}
