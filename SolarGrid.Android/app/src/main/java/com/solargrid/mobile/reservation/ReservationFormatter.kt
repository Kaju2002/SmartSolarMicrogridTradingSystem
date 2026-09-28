/*
 * File: ReservationFormatter.kt
 * Module: Reservation Management (Kajanthan)
 * Description: kWh and LKR numbers for the booking screens, e.g. "12.5" and "1,250"
 */
package com.solargrid.mobile.reservation

import java.text.DecimalFormat

object ReservationFormatter {

    // Up to two decimals, no trailing zeros, thousands separator
    fun number(value: Double): String = DecimalFormat("#,##0.##").format(value)
}
