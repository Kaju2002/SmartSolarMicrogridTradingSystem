/*
 * File: OperatorStationAdapter.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Station cards for the Grid Operator Stations tab, with the booking counts
 *              at each station and a − / + stepper for free battery slots.
 *              Station values use the shared StationFormatter.
 */
package com.solargrid.mobile.verification.operator

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemOperatorStationBinding
import com.solargrid.mobile.station.StationFormatter
import com.solargrid.mobile.verification.operator.models.OperatorStation

class OperatorStationAdapter(
    private val onStationClick: (OperatorStation) -> Unit,
    private val onSlotsChange: (OperatorStation, Int) -> Unit
) : RecyclerView.Adapter<OperatorStationAdapter.StationViewHolder>() {

    // Pending and approved-today counts for one station
    data class Counts(val pending: Int, val approvedToday: Int)

    private var stations: List<OperatorStation> = emptyList()
    private var counts: Map<String, Counts>? = null
    private val saving = mutableSetOf<String>()

    // Replace the list; counts are null when the bookings couldn't be loaded
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newStations: List<OperatorStation>, newCounts: Map<String, Counts>?) {
        stations = newStations
        counts = newCounts
        notifyDataSetChanged()
    }

    // Stepper locked while a station's free slots are being saved
    fun setSaving(stationId: String, inProgress: Boolean) {
        if (inProgress) saving.add(stationId) else saving.remove(stationId)
        refresh(stationId)
    }

    // New free-slot number from the API
    fun updateSlots(stationId: String, availableSlots: Int) {
        stations = stations.map { if (it.id == stationId) it.copy(availableSlots = availableSlots) else it }
        refresh(stationId)
    }

    // Redraw one card
    private fun refresh(stationId: String) {
        val position = stations.indexOfFirst { it.id == stationId }
        if (position >= 0) notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val binding = ItemOperatorStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position])
    }

    override fun getItemCount(): Int = stations.size

    inner class StationViewHolder(private val binding: ItemOperatorStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Tap goes to the Pending bookings; − / + ask for one slot fewer or more
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onStationClick(stations[position])
            }
            binding.btnSlotsLess.setOnClickListener { stepSlots(-1) }
            binding.btnSlotsMore.setOnClickListener { stepSlots(+1) }
        }

        // Asks the screen to save the new number; the card changes once the API agrees
        private fun stepSlots(delta: Int) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val station = stations[position]
            val free = station.availableSlots ?: return
            val total = station.batterySlots ?: return
            val next = free + delta
            if (next in 0..total) onSlotsChange(station, next)
        }

        // Fill one card
        fun bind(operatorStation: OperatorStation) {
            val context = binding.root.context
            val station = operatorStation.toStation()

            binding.tvOperatorStationName.text = StationFormatter.name(context, station)
            binding.tvOperatorStationHours.text =
                StationFormatter.hours(context, station) ?: context.getString(R.string.station_value_unknown)
            binding.tvOperatorStationCapacity.text = StationFormatter.capacity(context, station)
            binding.tvOperatorStationRate.text = StationFormatter.rate(context, station)
            binding.tvOperatorStationSlots.text = StationFormatter.slots(context, station)

            // Deactivated stations and unknown numbers can't be changed; stop at 0 and at the total
            val free = operatorStation.availableSlots
            val total = operatorStation.batterySlots
            val canEdit = operatorStation.isActive() && free != null && total != null &&
                operatorStation.id !in saving
            binding.btnSlotsLess.isEnabled = canEdit && (free ?: 0) > 0
            binding.btnSlotsMore.isEnabled = canEdit && (free ?: 0) < (total ?: 0)

            // A deactivated station takes no bookings, whatever its hours say
            if (operatorStation.isActive()) {
                StationFormatter.bindStatus(binding.tvOperatorStationStatus, station)
            } else {
                val pill = binding.tvOperatorStationStatus
                pill.isVisible = true
                pill.setText(R.string.operator_station_deactivated)
                pill.setTextColor(ContextCompat.getColor(context, R.color.station_closed))
                pill.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.station_closed_bg))
            }

            val stationCounts = counts?.let { it[operatorStation.id] ?: Counts(0, 0) }
            binding.tvOperatorStationBookings.isVisible = stationCounts != null
            if (stationCounts != null) {
                binding.tvOperatorStationBookings.text = context.getString(
                    R.string.operator_station_bookings, stationCounts.pending, stationCounts.approvedToday
                )
            }
        }
    }
}
