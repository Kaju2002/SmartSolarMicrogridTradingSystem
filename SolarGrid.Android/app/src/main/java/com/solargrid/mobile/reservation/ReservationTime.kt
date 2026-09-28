/*
 * File: ReservationTime.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Reads API booking times, builds the dates the availability call expects,
 *              and formats Sri Lanka dates for the booking screens
 */
package com.solargrid.mobile.reservation

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReservationTime {

    // Slot dates and labels are in Sri Lanka time, like the API
    val SRI_LANKA: TimeZone = TimeZone.getTimeZone("Asia/Colombo")

    private const val API_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss"
    private const val API_DATE = "yyyy-MM-dd"

    // Length of "2026-09-29T03:30:00"
    private const val DATE_TIME_LENGTH = 19

    // The API takes bookings up to 7 days ahead
    const val BOOKABLE_DAYS = 7

    // API times are UTC; fractions of a second and the trailing "Z" are ignored
    fun parseUtc(text: String?): Long? {
        val value = text?.trim()?.take(DATE_TIME_LENGTH) ?: return null
        return try {
            format(API_DATE_TIME, TimeZone.getTimeZone("UTC")).parse(value)?.time
        } catch (e: ParseException) {
            null
        }
    }

    // "yyyy-MM-dd" of an instant in Sri Lanka, for the availability call
    fun apiDate(millis: Long): String = format(API_DATE, SRI_LANKA).format(Date(millis))

    // Sri Lanka midnight of today and the following days, one per day chip
    fun bookableDays(count: Int = BOOKABLE_DAYS): List<Long> {
        val today = Calendar.getInstance(SRI_LANKA).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return List(count) { offset ->
            (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }.timeInMillis
        }
    }

    // "Tue"
    fun dayName(millis: Long): String = display("EEE", millis)

    // "29"
    fun dayNumber(millis: Long): String = display("d", millis)

    // "Sep"
    fun monthName(millis: Long): String = display("MMM", millis)

    // "Tue, 29 Sep"
    fun dayLabel(millis: Long): String = display("EEE, d MMM", millis)

    // Sri Lanka date text in the phone's language
    private fun display(pattern: String, millis: Long): String =
        format(pattern, SRI_LANKA, Locale.getDefault()).format(Date(millis))

    // SimpleDateFormat is not thread safe, so each call gets its own
    private fun format(pattern: String, zone: TimeZone, locale: Locale = Locale.US) =
        SimpleDateFormat(pattern, locale).apply {
            timeZone = zone
            isLenient = false
        }
}
