/*
 * File: OperatorBookingFormatter.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator booking groups (pending, approved, completed, and today's)
 *              and the booking card used on the operator Home and Bookings tabs.
 *              Days are Sri Lanka days, like the API.
 */
package com.solargrid.mobile.verification.operator

import androidx.core.view.isVisible
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemOperatorBookingBinding
import com.solargrid.mobile.reservation.ReservationFormatter
import com.solargrid.mobile.reservation.ReservationTime
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.verification.operator.models.OperatorBooking

object OperatorBookingFormatter {

    // Slot start in millis, or null when the API time can't be read
    fun slotStart(booking: OperatorBooking): Long? = ReservationTime.parseUtc(booking.reservationDateTime)

    // True when the slot is on the same Sri Lanka day as now
    fun isToday(booking: OperatorBooking, now: Long = System.currentTimeMillis()): Boolean {
        val start = slotStart(booking) ?: return false
        return ReservationTime.apiDate(start) == ReservationTime.apiDate(now)
    }

    // Pending bookings that can still be approved, soonest first
    fun pending(bookings: List<OperatorBooking>, now: Long = System.currentTimeMillis()) =
        bookings.filter { canApprove(it, now) }.sortedBy { slotStart(it) }

    // Approved for today and not over yet, so the prosumer still has a QR to show
    fun approvedToday(bookings: List<OperatorBooking>, now: Long = System.currentTimeMillis()) =
        bookings.filter {
            it.status == Reservation.STATUS_APPROVED && isToday(it, now) &&
                !ReservationFormatter.isPast(it.toReservation(), now)
        }.sortedBy { slotStart(it) }

    // Approved on any day and not over yet, soonest first
    fun approvedUpcoming(bookings: List<OperatorBooking>, now: Long = System.currentTimeMillis()) =
        bookings.filter {
            it.status == Reservation.STATUS_APPROVED && !ReservationFormatter.isPast(it.toReservation(), now)
        }.sortedBy { slotStart(it) }

    // Transfers done today (scans only work on the slot day)
    fun completedToday(bookings: List<OperatorBooking>, now: Long = System.currentTimeMillis()) =
        bookings.filter { it.status == Reservation.STATUS_COMPLETED && isToday(it, now) }

    // Every completed transfer, latest first
    fun completed(bookings: List<OperatorBooking>) =
        bookings.filter { it.status == Reservation.STATUS_COMPLETED }.sortedByDescending { slotStart(it) }

    // Only a Pending booking whose hour hasn't passed can be approved (same check as the API)
    fun canApprove(booking: OperatorBooking, now: Long = System.currentTimeMillis()): Boolean =
        booking.status == Reservation.STATUS_PENDING && !ReservationFormatter.isPast(booking.toReservation(), now)

    // Fill one card; today's list shows only the hours, other lists the day too
    fun bind(card: ItemOperatorBookingBinding, booking: OperatorBooking, showDay: Boolean) {
        val context = card.root.context
        val reservation = booking.toReservation()
        val start = slotStart(booking)

        card.tvOperatorBookingTime.text = when {
            start == null -> context.getString(R.string.station_value_unknown)
            showDay -> ReservationFormatter.timeRange(context, reservation)
            else -> context.getString(
                R.string.operator_booking_hours,
                ReservationTime.clock(start),
                ReservationTime.clock(start + ReservationTime.SLOT_LENGTH_MS)
            )
        }
        ReservationFormatter.bindStatus(card.tvOperatorBookingStatus, reservation)

        card.tvOperatorBookingNic.text = context.getString(
            R.string.operator_booking_nic,
            booking.prosumerNic?.takeIf { it.isNotBlank() } ?: context.getString(R.string.station_value_unknown)
        )
        card.tvOperatorBookingStation.text =
            booking.stationName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.station_unnamed)

        val energy = ReservationFormatter.energy(context, reservation)
        card.tvOperatorBookingEnergy.text = energy
        card.tvOperatorBookingEnergy.isVisible = energy != null
    }
}
