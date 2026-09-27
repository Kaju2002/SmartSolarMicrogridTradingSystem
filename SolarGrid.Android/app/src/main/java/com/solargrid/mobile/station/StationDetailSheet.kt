/*
 * File: StationDetailSheet.kt
 * Module: Station Management (Gabilan)
 * Description: Bottom sheet with one station's details. Opens with the data already on the map,
 *              refreshes slots and hours from the API, and hands "Book a slot" back to the Stations tab.
 */
package com.solargrid.mobile.station

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemStationInfoBinding
import com.solargrid.mobile.databinding.SheetStationDetailBinding
import com.solargrid.mobile.station.models.Station
import kotlinx.coroutines.launch

class StationDetailSheet : BottomSheetDialogFragment() {

    private var _binding: SheetStationDetailBinding? = null
    private val binding get() = _binding!!

    private val stationId: String get() = requireArguments().getString(ARG_STATION_ID).orEmpty()
    private var station: Station? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetStationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Show what we already know right away, then ask the API for fresh numbers
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnDirections.setOnClickListener { openDirections() }
        binding.btnBookSlot.setOnClickListener { bookSlot() }

        val cached = StationManager.getInstance().getCachedStation(stationId)
        if (cached != null) showStation(cached) else showEmpty()
        refresh()
    }

    // Open fully; the sheet is short so there is no half state
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Keep the map data if the refresh fails; close only when there is nothing to show
    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            StationManager.getInstance().getStation(stationId)
                .onSuccess { showStation(it) }
                .onFailure { error ->
                    if (station == null) {
                        Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
        }
    }

    private fun showStation(value: Station) {
        station = value
        val context = requireContext()

        binding.tvDetailName.text = StationFormatter.name(context, value)
        StationFormatter.bindStatus(binding.tvDetailStatus, value)
        binding.tvDetailDistance.text =
            getString(R.string.station_distance_away, StationFormatter.distance(context, value))

        val unknown = getString(R.string.station_value_unknown)
        bindInfo(binding.infoHours, R.drawable.ic_schedule, R.string.station_info_hours,
            StationFormatter.hours(context, value) ?: unknown)
        bindInfo(binding.infoPrice, R.drawable.ic_payments, R.string.station_info_price,
            StationFormatter.rate(context, value))
        bindInfo(binding.infoSlots, R.drawable.ic_battery, R.string.station_info_slots,
            StationFormatter.slots(context, value))
        bindInfo(binding.infoCapacity, R.drawable.ic_bolt, R.string.station_info_capacity,
            StationFormatter.capacity(context, value))

        binding.btnDirections.isEnabled = true
        binding.btnBookSlot.isEnabled = true
    }

    // Opened after process death with no map data yet: blank tiles until the API answers
    private fun showEmpty() {
        val unknown = getString(R.string.station_value_unknown)
        binding.tvDetailStatus.visibility = View.GONE
        bindInfo(binding.infoHours, R.drawable.ic_schedule, R.string.station_info_hours, unknown)
        bindInfo(binding.infoPrice, R.drawable.ic_payments, R.string.station_info_price, unknown)
        bindInfo(binding.infoSlots, R.drawable.ic_battery, R.string.station_info_slots, unknown)
        bindInfo(binding.infoCapacity, R.drawable.ic_bolt, R.string.station_info_capacity, unknown)
        binding.btnDirections.isEnabled = false
        binding.btnBookSlot.isEnabled = false
    }

    private fun bindInfo(tile: ItemStationInfoBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        tile.ivInfoIcon.setImageResource(icon)
        tile.tvInfoLabel.setText(label)
        tile.tvInfoValue.text = value
    }

    // Google Maps directions (app if installed, otherwise the browser)
    private fun openDirections() {
        val value = station ?: return
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${value.latitude},${value.longitude}"
        )
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.station_no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }

    // The Stations tab decides where booking goes
    private fun bookSlot() {
        val result = Bundle().apply { putString(KEY_STATION_ID, stationId) }
        parentFragmentManager.setFragmentResult(REQUEST_BOOK_SLOT, result)
        dismiss()
    }

    companion object {
        const val TAG = "station_detail"
        const val REQUEST_BOOK_SLOT = "station_book_slot"
        const val KEY_STATION_ID = "station_id"
        private const val ARG_STATION_ID = "station_id"

        fun newInstance(stationId: String) = StationDetailSheet().apply {
            arguments = Bundle().apply { putString(ARG_STATION_ID, stationId) }
        }
    }
}
