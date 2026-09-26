/*
 * File: BookingUiModels.kt
 * Description: My-booking card UI model
 */
package com.solargrid.prosumer.ui.bookings

import androidx.annotation.DrawableRes
import com.solargrid.prosumer.R

data class BookingCardUi(
    val id: String,
    val stationId: String,
    val stationName: String,
    val location: String,
    val status: String,
    val slotLabel: String,
    val qrCode: String? = null,
    @DrawableRes val imageRes: Int = R.drawable.station_house,
)
