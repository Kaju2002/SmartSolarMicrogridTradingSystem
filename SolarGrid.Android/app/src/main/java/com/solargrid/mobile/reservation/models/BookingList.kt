/*
 * File: BookingList.kt
 * Module: Reservation Management (Kajanthan)
 * Description: What the Bookings tab shows: the bookings, their station names, and whether
 *              they came from the phone's saved copy because the API couldn't be reached.
 */
package com.solargrid.mobile.reservation.models

data class BookingList(
    // Earliest slot first
    val bookings: List<Reservation>,
    // Station id -> name
    val stationNames: Map<String, String>,
    // True when read from SQLite instead of the API
    val offline: Boolean,
    // Epoch millis of the last online load
    val savedAt: Long
)
