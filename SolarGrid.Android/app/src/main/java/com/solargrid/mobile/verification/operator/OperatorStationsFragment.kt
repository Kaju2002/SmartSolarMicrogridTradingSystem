/*
 * File: OperatorStationsFragment.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator Stations tab. Stations assigned to the operator with hours,
 *              capacity, rate, free slots and booking counts. View only; Backoffice edits
 *              stations on the web. Tapping a card opens the Pending bookings.
 */
package com.solargrid.mobile.verification.operator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.FragmentOperatorStationsBinding
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class OperatorStationsFragment : Fragment() {

    private var _binding: FragmentOperatorStationsBinding? = null
    private val binding get() = _binding!!

    private var hasData = false
    private var loadJob: Job? = null

    private val listAdapter = OperatorStationAdapter {
        (activity as? OperatorMainActivity)?.openBookings(OperatorBookingsFragment.Filter.PENDING)
    }

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // List, pull-to-refresh and retry, then load
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOperatorStations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOperatorStations.adapter = listAdapter

        binding.swipeOperatorStations.setColorSchemeResources(R.color.accent_gold)
        binding.swipeOperatorStations.setOnRefreshListener { load(showSpinner = false) }
        // Only pull to refresh when the list is at the top
        binding.swipeOperatorStations.setOnChildScrollUpCallback { _, _ ->
            binding.rvOperatorStations.isVisible && binding.rvOperatorStations.canScrollVertically(-1)
        }
        binding.btnOperatorStationsRetry.setOnClickListener { load(showSpinner = true) }
        load(showSpinner = true)
    }

    // Tabs are hidden, not destroyed; coming back refreshes the counts
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) load(showSpinner = !hasData)
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Stations and bookings together; the cards still show if only the bookings fail
    private fun load(showSpinner: Boolean) {
        loadJob?.cancel()
        if (showSpinner && !binding.swipeOperatorStations.isRefreshing) {
            binding.progressOperatorStations.isVisible = true
            binding.rvOperatorStations.isVisible = false
            binding.layoutOperatorStationsEmpty.isVisible = false
        }

        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val manager = OperatorManager.getInstance()
            val stationsCall = async { manager.getMyStations() }
            val bookingsCall = async { manager.getBookings() }
            val stations = stationsCall.await()
            val counts = bookingsCall.await().getOrNull()?.let { countsByStation(it) }

            binding.progressOperatorStations.isVisible = false
            binding.swipeOperatorStations.isRefreshing = false

            stations
                .onSuccess { list ->
                    hasData = true
                    if (list.isEmpty()) {
                        showMessage(getString(R.string.operator_home_no_station), retry = false)
                    } else {
                        binding.layoutOperatorStationsEmpty.isVisible = false
                        binding.rvOperatorStations.isVisible = true
                        listAdapter.submit(list, counts)
                    }
                }
                .onFailure { error ->
                    // Keep the old cards on a refresh failure; the next pull tries again
                    if (!hasData) {
                        showMessage(error.message ?: getString(R.string.operator_error_stations), retry = true)
                    }
                }
        }
    }

    // Pending and approved-today counts per station id
    private fun countsByStation(bookings: List<OperatorBooking>): Map<String, OperatorStationAdapter.Counts> {
        val now = System.currentTimeMillis()
        val pending = OperatorBookingFormatter.pending(bookings, now).groupingBy { it.stationId }.eachCount()
        val approved = OperatorBookingFormatter.approvedToday(bookings, now).groupingBy { it.stationId }.eachCount()
        return (pending.keys + approved.keys).filterNotNull().associateWith { id ->
            OperatorStationAdapter.Counts(pending[id] ?: 0, approved[id] ?: 0)
        }
    }

    // No stations or a loading error; retry only for errors
    private fun showMessage(message: String, retry: Boolean) {
        binding.rvOperatorStations.isVisible = false
        binding.layoutOperatorStationsEmpty.isVisible = true
        binding.tvOperatorStationsEmpty.text = message
        binding.btnOperatorStationsRetry.isVisible = retry
    }
}
