/*
 * File: BookingUiModels.kt
 * Description: My-booking card UI model
 */
package com.solargrid.prosumer.ui.bookings

import androidx.annotation.DrawableRes
import com.solargrid.prosumer.R

/** API blocks change/cancel inside this window before the slot. */
const val CHANGE_LOCK_HOURS = 12

data class BookingCardUi(
    val id: String,
    val stationId: String,
    val stationName: String,
    val location: String,
    val status: String,
    val slotLabel: String,
    /** Slot start in epoch millis; 0 when unknown. */
    val slotMillis: Long = 0L,
    val qrCode: String? = null,
    @DrawableRes val imageRes: Int = R.drawable.station_house,
) {
    val isActive: Boolean
        get() = status.equals("Pending", ignoreCase = true) ||
            status.equals("Approved", ignoreCase = true)

    val isApproved: Boolean
        get() = status.equals("Approved", ignoreCase = true)

    fun canChange(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isActive && slotMillis > 0 &&
            slotMillis - nowMillis >= CHANGE_LOCK_HOURS * 60 * 60 * 1000L
}
