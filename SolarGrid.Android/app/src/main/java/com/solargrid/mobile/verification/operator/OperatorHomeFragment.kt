/*
 * File: OperatorHomeFragment.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator Home tab. Greets the operator, lists the stations assigned
 *              to them, opens the QR scanner, shows Pending / Approved today /
 *              Completed today counts and today's approved bookings.
 */
package com.solargrid.mobile.verification.operator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginManager
import com.solargrid.mobile.databinding.FragmentOperatorHomeBinding
import com.solargrid.mobile.databinding.ItemOperatorBookingBinding
import com.solargrid.mobile.verification.dashboard.DashboardTiles
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import com.solargrid.mobile.verification.operator.models.OperatorStation
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class OperatorHomeFragment : Fragment() {

    private var _binding: FragmentOperatorHomeBinding? = null
    private val binding get() = _binding!!

    private var loadJob: Job? = null
    private var hasLoaded = false

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Tiles, pull to refresh and taps; data loads in onResume
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasLoaded = false
        setupTiles()
        binding.swipeOperatorHome.setColorSchemeResources(R.color.accent_gold)
        binding.swipeOperatorHome.setOnRefreshListener { load() }
        binding.btnScanQr.setOnClickListener {
            startActivity(Intent(requireContext(), ScanQrActivity::class.java))
        }
        // A booking cancelled from the sheet leaves today's list straight away
        childFragmentManager.setFragmentResultListener(OperatorBookingSheet.REQUEST_CANCELLED, viewLifecycleOwner) { _, _ ->
            load()
        }
    }

    // First open and coming back from the scanner, so a finished scan shows straight away
    override fun onResume() {
        super.onResume()
        if (!isHidden) load()
    }

    // Tabs are hidden, not destroyed, so refresh when the operator comes back
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) load()
    }

    // Same colours as the booking pills; dashes until the first load.
    // "Completed today" wraps on phones, so every label keeps two lines and the tiles line up.
    private fun setupTiles() {
        DashboardTiles.paint(binding.tileOperatorPending, R.string.operator_tile_pending,
            R.color.booking_pending, R.color.booking_pending_bg)
        DashboardTiles.paint(binding.tileOperatorApproved, R.string.operator_tile_approved_today,
            R.color.station_open, R.color.station_open_bg)
        DashboardTiles.paint(binding.tileOperatorCompleted, R.string.operator_tile_completed_today,
            R.color.booking_completed, R.color.booking_completed_bg)

        mapOf(
            binding.tileOperatorPending to OperatorBookingsFragment.Filter.PENDING,
            binding.tileOperatorApproved to OperatorBookingsFragment.Filter.APPROVED,
            binding.tileOperatorCompleted to OperatorBookingsFragment.Filter.COMPLETED
        ).forEach { (tile, filter) ->
            DashboardTiles.setCount(tile, null)
            tile.tvCountLabel.minLines = 2
            tile.root.setOnClickListener { (activity as? OperatorMainActivity)?.openBookings(filter) }
        }
    }

    // Details of one of today's bookings
    private fun openDetail(booking: OperatorBooking) {
        if (childFragmentManager.findFragmentByTag(OperatorBookingSheet.TAG) != null) return
        OperatorBookingSheet.newInstance(booking).show(childFragmentManager, OperatorBookingSheet.TAG)
    }

    // Saved name first so the header is never empty, then stations and bookings together.
    // On failure the last numbers stay and the message asks to pull down.
    private fun load() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession() ?: return@launch
            binding.tvOperatorGreeting.text =
                getString(R.string.operator_home_greeting, user.fullName.trim())

            if (!hasLoaded) binding.swipeOperatorHome.isRefreshing = true
            val manager = OperatorManager.getInstance()
            val stationsCall = async { manager.getMyStations() }
            val bookingsCall = async { manager.getBookings() }
            val stations = stationsCall.await()
            val bookings = bookingsCall.await()
            binding.swipeOperatorHome.isRefreshing = false

            stations.onSuccess { showStations(it) }
            bookings.onSuccess {
                showBookings(it)
                hasLoaded = true
            }

            val error = stations.exceptionOrNull() ?: bookings.exceptionOrNull()
            binding.tvOperatorHomeMessage.isVisible = error != null
            if (error != null) {
                binding.tvOperatorHomeMessage.text = getString(
                    R.string.operator_error_retry,
                    error.message ?: getString(R.string.operator_error_bookings)
                )
            }
        }
    }

    // Station names under the greeting, or a hint to ask Backoffice
    private fun showStations(stations: List<OperatorStation>) {
        val names = stations.mapNotNull { it.stationName?.takeIf(String::isNotBlank) }
        binding.tvOperatorStations.text = if (names.isEmpty()) {
            getString(R.string.operator_home_no_station)
        } else {
            names.joinToString(", ")
        }
    }

    // Counts in the tiles and one card per approved booking still to come today
    private fun showBookings(bookings: List<OperatorBooking>) {
        val now = System.currentTimeMillis()
        val approvedToday = OperatorBookingFormatter.approvedToday(bookings, now)

        DashboardTiles.setCount(binding.tileOperatorPending, OperatorBookingFormatter.pending(bookings, now).size)
        DashboardTiles.setCount(binding.tileOperatorApproved, approvedToday.size)
        DashboardTiles.setCount(binding.tileOperatorCompleted, OperatorBookingFormatter.completedToday(bookings, now).size)

        binding.layoutTodayBookings.removeAllViews()
        approvedToday.forEach { booking ->
            val card = ItemOperatorBookingBinding.inflate(layoutInflater, binding.layoutTodayBookings, false)
            OperatorBookingFormatter.bind(card, booking, showDay = false)
            card.root.setOnClickListener { openDetail(booking) }
            binding.layoutTodayBookings.addView(card.root)
        }
        binding.tvTodayEmpty.isVisible = approvedToday.isEmpty()
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
