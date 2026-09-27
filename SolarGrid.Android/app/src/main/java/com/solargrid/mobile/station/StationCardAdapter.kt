/*
 * File: StationCardAdapter.kt
 * Module: Station Management (Gabilan)
 * Description: Cards for the station carousel on the map. The selected card gets a gold outline.
 */
package com.solargrid.mobile.station

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemStationCardBinding
import com.solargrid.mobile.station.models.Station

class StationCardAdapter(
    private val onCardClick: (position: Int) -> Unit
) : RecyclerView.Adapter<StationCardAdapter.CardViewHolder>() {

    private var stations: List<Station> = emptyList()
    private var selectedPosition = RecyclerView.NO_POSITION

    // Replace the list and pick the selected card
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newStations: List<Station>, selected: Int) {
        stations = newStations
        selectedPosition = selected
        notifyDataSetChanged()
    }

    // Move the gold outline to another card
    fun setSelected(position: Int) {
        if (position == selectedPosition) return
        val old = selectedPosition
        selectedPosition = position
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemStationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(stations[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = stations.size

    inner class CardViewHolder(private val binding: ItemStationCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onCardClick(position)
            }
        }

        // Fill one card
        fun bind(station: Station, selected: Boolean) {
            val context = binding.root.context

            binding.tvStationName.text = StationFormatter.name(context, station)
            val hours = StationFormatter.hours(context, station)
            binding.tvStationHours.isVisible = hours != null
            binding.tvStationHours.text = hours
            StationFormatter.bindStatus(binding.tvStationStatus, station)

            binding.tvDistance.text = StationFormatter.distance(context, station)
            binding.tvRate.text = StationFormatter.rate(context, station)
            binding.tvSlots.text = StationFormatter.slots(context, station)

            binding.root.strokeWidth =
                if (selected) context.resources.getDimensionPixelSize(R.dimen.station_card_stroke) else 0
        }
    }
}
