/*
 * File: OperatorStationAdapter.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Station cards for the Grid Operator Stations tab, with the booking counts
 *              at each station. Station values use the shared StationFormatter.
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
    private val onStationClick: (OperatorStation) -> Unit
) : RecyclerView.Adapter<OperatorStationAdapter.StationViewHolder>() {

    // Pending and approved-today counts for one station
    data class Counts(val pending: Int, val approvedToday: Int)

    private var stations: List<OperatorStation> = emptyList()
    private var counts: Map<String, Counts>? = null

    // Replace the list; counts are null when the bookings couldn't be loaded
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newStations: List<OperatorStation>, newCounts: Map<String, Counts>?) {
        stations = newStations
        counts = newCounts
        notifyDataSetChanged()
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

        // Tap goes to the Pending bookings
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onStationClick(stations[position])
            }
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
