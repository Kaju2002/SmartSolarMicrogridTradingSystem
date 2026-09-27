/*
 * File: GeoUtils.kt
 * Description: Small map maths helpers
 */
package com.solargrid.mobile.core.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng

// Straight-line distance between two points in kilometres
fun distanceKm(from: LatLng, to: LatLng): Double {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
    return result[0] / 1000.0
}
