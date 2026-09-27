/*
 * File: StationFormatter.kt
 * Module: Station Management (Gabilan)
 * Description: Turns station values into display text, shared by the map cards and the detail sheet.
 *              Unknown values show a dash.
 */
package com.solargrid.mobile.station

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.solargrid.mobile.R
import com.solargrid.mobile.station.models.Station
import java.text.DecimalFormat
import java.util.Calendar
import kotlin.math.roundToInt

object StationFormatter {

    private val DISTANCE_FORMAT = DecimalFormat("0.0")
    private val NUMBER_FORMAT = DecimalFormat("#,##0.##")

    fun name(context: Context, station: Station): String =
        station.stationName ?: context.getString(R.string.station_unnamed)

    // "06:00 – 18:00", or null when the hours are unknown
    fun hours(context: Context, station: Station): String? {
        val open = station.openTime ?: return null
        val close = station.closeTime ?: return null
        return context.getString(R.string.station_hours, open, close)
    }

    // "850 m" under a kilometre, "2.2 km" above
    fun distance(context: Context, station: Station): String {
        val km = station.distanceKm ?: return unknown(context)
        return if (km < 1.0) {
            context.getString(R.string.station_distance_m, (km * 100).roundToInt() * 10)
        } else {
            context.getString(R.string.station_distance_km, DISTANCE_FORMAT.format(km))
        }
    }

    // "LKR 45"
    fun rate(context: Context, station: Station): String {
        val rate = station.ratePerKwh ?: return unknown(context)
        return context.getString(R.string.station_rate, NUMBER_FORMAT.format(rate))
    }

    // "3 of 5"
    fun slots(context: Context, station: Station): String {
        val free = station.availableSlots ?: return unknown(context)
        val total = station.batterySlots ?: return unknown(context)
        return context.getString(R.string.station_slots, free, total)
    }

    // "120 kWh"
    fun capacity(context: Context, station: Station): String {
        val capacity = station.capacityKWh ?: return unknown(context)
        return context.getString(R.string.station_capacity, NUMBER_FORMAT.format(capacity))
    }

    // Green "Open now" or grey "Closed" pill, hidden when the hours are unknown
    fun bindStatus(pill: TextView, station: Station) {
        val hasHours = station.openTime != null && station.closeTime != null
        pill.isVisible = hasHours
        if (!hasHours) return

        val context = pill.context
        val open = station.isOpenAt(minuteOfDayNow())
        pill.setText(if (open) R.string.station_open_now else R.string.station_closed)
        pill.setTextColor(ContextCompat.getColor(context, if (open) R.color.station_open else R.color.station_closed))
        pill.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (open) R.color.station_open_bg else R.color.station_closed_bg)
        )
    }

    private fun unknown(context: Context) = context.getString(R.string.station_value_unknown)

    private fun minuteOfDayNow(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }
}
