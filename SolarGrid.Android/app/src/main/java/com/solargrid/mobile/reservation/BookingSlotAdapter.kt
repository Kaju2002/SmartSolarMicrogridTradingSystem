/*
 * File: BookingSlotAdapter.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Hourly time chips with the kWh still free. Full and past slots are grey and can't be tapped.
 */
package com.solargrid.mobile.reservation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemBookingSlotBinding
import com.solargrid.mobile.reservation.models.BookingSlot

class BookingSlotAdapter(
    private val onSlotClick: (BookingSlot) -> Unit
) : RecyclerView.Adapter<BookingSlotAdapter.SlotViewHolder>() {

    private var slots: List<BookingSlot> = emptyList()

    // slotDateTime of the picked slot
    private var selectedTime: String? = null

    // Replace the slots and pick one (or none)
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newSlots: List<BookingSlot>, selected: String?) {
        slots = newSlots
        selectedTime = selected
        notifyDataSetChanged()
    }

    // Move the gold highlight to another slot
    fun setSelected(slotDateTime: String?) {
        val old = slots.indexOfFirst { it.slotDateTime == selectedTime }
        selectedTime = slotDateTime
        if (old >= 0) notifyItemChanged(old)
        val new = slots.indexOfFirst { it.slotDateTime == slotDateTime }
        if (new >= 0) notifyItemChanged(new)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemBookingSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        holder.bind(slot, slot.slotDateTime == selectedTime)
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(private val binding: ItemBookingSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onSlotClick(slots[position])
            }
        }

        // Fill one chip; only Available slots are enabled
        fun bind(slot: BookingSlot, selected: Boolean) {
            val context = binding.root.context
            val available = slot.isAvailable()

            binding.tvSlotTime.text = slot.startTime
            binding.tvSlotSpots.text = when {
                available -> context.getString(
                    R.string.booking_kwh_left, ReservationFormatter.number(slot.kWhLeft ?: 0.0)
                )
                slot.status == BookingSlot.STATUS_FULL -> context.getString(R.string.booking_slot_full)
                slot.status == BookingSlot.STATUS_CURRENT -> context.getString(R.string.booking_slot_current)
                else -> context.getString(R.string.booking_slot_unavailable)
            }
            binding.root.isEnabled = available
            binding.root.isSelected = selected && available
        }
    }
}
