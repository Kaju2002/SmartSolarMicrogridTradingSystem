/*
 * File: DeviceLocationManager.kt
 * Description: Singleton that reads the phone's current location with the fused location provider
 */
package com.solargrid.mobile.core.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class DeviceLocationManager private constructor() {

    private val context get() = ContextManager.getInstance().getApplicationContext()

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // True when the user allowed precise or approximate location
    fun hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    // Where the phone is now; null without permission, with location turned off, or after the timeout
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) { freshLocation() } ?: lastKnownLocation()
        } catch (e: SecurityException) {
            null
        }
    }

    // Ask the provider for a new fix; GPS is only used when precise location was allowed
    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(): LatLng? = suspendCancellableCoroutine { continuation ->
        val preciseAllowed = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val priority = if (preciseAllowed) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val cancelSource = CancellationTokenSource()
        fusedClient.getCurrentLocation(priority, cancelSource.token)
            .addOnCompleteListener { task ->
                continuation.resume(if (task.isSuccessful) task.result?.toLatLng() else null)
            }
        continuation.invokeOnCancellation { cancelSource.cancel() }
    }

    // Last fix another app got, used when a new fix is slow
    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): LatLng? = suspendCancellableCoroutine { continuation ->
        fusedClient.lastLocation.addOnCompleteListener { task ->
            continuation.resume(if (task.isSuccessful) task.result?.toLatLng() else null)
        }
    }

    private fun Location.toLatLng() = LatLng(latitude, longitude)

    companion object {
        // Asked together so Android 12+ can offer "approximate" as well
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private const val LOCATION_TIMEOUT_MS = 8_000L

        @Volatile
        private var instance: DeviceLocationManager? = null

        // One shared DeviceLocationManager for the app
        fun getInstance(): DeviceLocationManager =
            instance ?: synchronized(this) {
                instance ?: DeviceLocationManager().also { instance = it }
            }
    }
}
