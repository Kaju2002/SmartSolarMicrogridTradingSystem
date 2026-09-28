/*
 * File: BookingDayAdapter.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Day chips on the booking screen. The first chip reads "Today"; the picked one is gold.
 */
package com.solargrid.mobile.reservation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemBookingDayBinding

class BookingDayAdapter(
    private val onDayClick: (position: Int) -> Unit
) : RecyclerView.Adapter<BookingDayAdapter.DayViewHolder>() {

    private var days: List<Long> = emptyList()
    private var selectedPosition = 0

    // Replace the days (Sri Lanka midnight millis) and pick one
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newDays: List<Long>, selected: Int) {
        days = newDays
        selectedPosition = selected
        notifyDataSetChanged()
    }

    // Move the gold highlight to another day
    fun setSelected(position: Int) {
        if (position == selectedPosition) return
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemBookingDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], position == 0, position == selectedPosition)
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(private val binding: ItemBookingDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onDayClick(position)
            }
        }

        // Fill one chip
        fun bind(day: Long, isToday: Boolean, selected: Boolean) {
            binding.tvDayName.text =
                if (isToday) binding.root.context.getString(R.string.booking_today) else ReservationTime.dayName(day)
            binding.tvDayNumber.text = ReservationTime.dayNumber(day)
            binding.tvDayMonth.text = ReservationTime.monthName(day)
            binding.root.isSelected = selected
        }
    }
}
