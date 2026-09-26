/*
 * File: BookingUiModels.kt
 * Description: Sample my-booking cards for Bookings tab
 */
package com.solargrid.prosumer.ui.bookings

import androidx.annotation.DrawableRes
import com.solargrid.prosumer.R

data class BookingCardUi(
    val id: String,
    val stationName: String,
    val location: String,
    val status: String,
    val slotLabel: String,
    val energyLabel: String,
    @DrawableRes val imageRes: Int,
)

val SampleBookings = listOf(
    BookingCardUi(
        id = "bk-1",
        stationName = "Solar Hub A",
        location = "Colombo · Microgrid",
        status = "Approved",
        slotLabel = "Today · 2:00 PM – 4:00 PM",
        energyLabel = "15 kWh",
        imageRes = R.drawable.station_house,
    ),
    BookingCardUi(
        id = "bk-2",
        stationName = "Green Station B",
        location = "Kandy · Community",
        status = "Pending",
        slotLabel = "Tomorrow · 10:00 AM – 12:00 PM",
        energyLabel = "10 kWh",
        imageRes = R.drawable.station_isometric,
    ),
    BookingCardUi(
        id = "bk-3",
        stationName = "Harbor Charge C",
        location = "Galle · Coastal",
        status = "Completed",
        slotLabel = "22 Mar · 9:00 AM – 11:00 AM",
        energyLabel = "20 kWh",
        imageRes = R.drawable.station_house,
    ),
)
