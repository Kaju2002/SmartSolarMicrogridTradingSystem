/*
 * File: OperatorBookingAdapter.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Booking cards for the Grid Operator Bookings tab, with the day shown.
 */
package com.solargrid.mobile.verification.operator

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.databinding.ItemOperatorBookingBinding
import com.solargrid.mobile.verification.operator.models.OperatorBooking

class OperatorBookingAdapter(
    private val onBookingClick: (OperatorBooking) -> Unit
) : RecyclerView.Adapter<OperatorBookingAdapter.BookingViewHolder>() {

    private var bookings: List<OperatorBooking> = emptyList()

    // Replace the list (already filtered and sorted)
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newBookings: List<OperatorBooking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemOperatorBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        OperatorBookingFormatter.bind(holder.binding, bookings[position], showDay = true)
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(val binding: ItemOperatorBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Tap opens the detail sheet
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onBookingClick(bookings[position])
            }
        }
    }
}
