/*
 * File: BookingListAdapter.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Booking cards for the Bookings tab. Station names come from the fragment,
 *              which looks them up once per station.
 */
package com.solargrid.mobile.reservation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemBookingCardBinding
import com.solargrid.mobile.reservation.models.Reservation

class BookingListAdapter(
    private val stationName: (stationId: String?) -> String?,
    private val onBookingClick: (Reservation) -> Unit
) : RecyclerView.Adapter<BookingListAdapter.BookingViewHolder>() {

    private var bookings: List<Reservation> = emptyList()

    // Replace the list (already filtered and sorted)
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newBookings: List<Reservation>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onBookingClick(bookings[position])
            }
        }

        // Fill one card
        fun bind(booking: Reservation) {
            val context = binding.root.context

            binding.tvBookingStation.text =
                stationName(booking.stationId) ?: context.getString(R.string.station_unnamed)
            ReservationFormatter.bindStatus(binding.tvBookingStatus, booking)
            binding.tvBookingTime.text = ReservationFormatter.timeRange(context, booking)

            val energy = ReservationFormatter.energy(context, booking)
            binding.tvBookingEnergy.isVisible = energy != null
            binding.tvBookingEnergy.text = energy
        }
    }
}
