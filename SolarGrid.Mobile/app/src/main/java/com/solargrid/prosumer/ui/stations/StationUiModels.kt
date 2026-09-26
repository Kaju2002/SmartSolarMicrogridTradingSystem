/*
 * File: StationUiModels.kt
 * Description: Sample station cards for Map/Book UI
 */
package com.solargrid.prosumer.ui.stations

import androidx.annotation.DrawableRes
import com.solargrid.prosumer.R

data class StationCardUi(
    val id: String,
    val name: String,
    val location: String,
    val capacityKw: String,
    val priceLabel: String,
    @DrawableRes val imageRes: Int,
)

val SampleStations = listOf(
    StationCardUi(
        id = "st-1",
        name = "Solar Hub A",
        location = "Colombo · Microgrid",
        capacityKw = "100 kW",
        priceLabel = "LKR 45 / kWh",
        imageRes = R.drawable.station_house,
    ),
    StationCardUi(
        id = "st-2",
        name = "Green Station B",
        location = "Kandy · Community",
        capacityKw = "75 kW",
        priceLabel = "LKR 42 / kWh",
        imageRes = R.drawable.station_isometric,
    ),
    StationCardUi(
        id = "st-3",
        name = "Harbor Charge C",
        location = "Galle · Coastal",
        capacityKw = "120 kW",
        priceLabel = "LKR 48 / kWh",
        imageRes = R.drawable.station_house,
    ),
)
