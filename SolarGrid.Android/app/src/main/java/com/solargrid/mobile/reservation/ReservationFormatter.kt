/*
 * File: ReservationFormatter.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Display text for bookings: kWh / LKR numbers, slot time, energy line and the
 *              coloured status pill. Shared by the booking screen and the Bookings list.
 */
package com.solargrid.mobile.reservation

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.solargrid.mobile.R
import com.solargrid.mobile.reservation.models.Reservation
import java.text.DecimalFormat

object ReservationFormatter {

    // Up to two decimals, no trailing zeros, thousands separator
    fun number(value: Double): String = DecimalFormat("#,##0.##").format(value)

    // Slot start in millis, or null when the API time can't be read
    fun slotStart(reservation: Reservation): Long? = ReservationTime.parseUtc(reservation.reservationDateTime)

    // True once the slot's hour is over
    fun isPast(reservation: Reservation, now: Long = System.currentTimeMillis()): Boolean {
        val start = slotStart(reservation) ?: return false
        return start + ReservationTime.SLOT_LENGTH_MS <= now
    }

    // Last moment the booking can be changed or cancelled
    fun changeDeadline(reservation: Reservation): Long? =
        slotStart(reservation)?.minus(ReservationTime.CHANGE_CUTOFF_MS)

    // Live and at least 12 hours before the slot, same check as the API
    fun canChange(reservation: Reservation, now: Long = System.currentTimeMillis()): Boolean {
        val deadline = changeDeadline(reservation) ?: return false
        return reservation.isLive() && now <= deadline
    }

    // "Tue, 29 Sep · 09:00 – 10:00", or a dash when the time is unknown
    fun timeRange(context: Context, reservation: Reservation): String {
        val start = slotStart(reservation) ?: return context.getString(R.string.station_value_unknown)
        return context.getString(
            R.string.booking_time_range,
            ReservationTime.dayLabel(start),
            ReservationTime.clock(start),
            ReservationTime.clock(start + ReservationTime.SLOT_LENGTH_MS)
        )
    }

    // "10 kWh · Estimated LKR 450", or null for bookings made before kWh was added
    fun energy(context: Context, reservation: Reservation): String? {
        val kWh = reservation.requestedKWh ?: 0.0
        if (kWh <= 0) return null
        return context.getString(R.string.booking_energy, number(kWh), number(reservation.estimatedCost ?: 0.0))
    }

    // Coloured pill; a Pending/Approved booking whose hour has passed shows as Expired
    fun bindStatus(pill: TextView, reservation: Reservation) {
        val (label, textColor, background) = statusStyle(reservation)
        paintPill(pill, label, textColor, background)
    }

    // Same label as the pill, so search matches what the card shows
    @StringRes
    fun statusLabel(reservation: Reservation): Int = statusStyle(reservation).first

    // Label, text colour and background for the status pill
    private fun statusStyle(reservation: Reservation): Triple<Int, Int, Int> = when {
        reservation.isLive() && isPast(reservation) ->
            Triple(R.string.booking_status_expired, R.color.station_closed, R.color.station_closed_bg)
        reservation.status == Reservation.STATUS_PENDING ->
            Triple(R.string.booking_status_pending, R.color.booking_pending, R.color.booking_pending_bg)
        reservation.status == Reservation.STATUS_APPROVED ->
            Triple(R.string.booking_status_approved, R.color.station_open, R.color.station_open_bg)
        reservation.status == Reservation.STATUS_COMPLETED ->
            Triple(R.string.booking_status_completed, R.color.booking_completed, R.color.booking_completed_bg)
        else ->
            Triple(R.string.booking_status_cancelled, R.color.station_closed, R.color.station_closed_bg)
    }

    private fun paintPill(pill: TextView, @StringRes label: Int, @ColorRes text: Int, @ColorRes background: Int) {
        val context = pill.context
        pill.setText(label)
        pill.setTextColor(ContextCompat.getColor(context, text))
        pill.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, background))
    }
}
