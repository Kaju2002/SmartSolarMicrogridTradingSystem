/*
 * File: StationManager.kt
 * Module: Station Management (Gabilan)
 * Description: Loads solar stations from the API for the map, list and detail screens
 */
package com.solargrid.mobile.station

import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.NetworkManager
import com.solargrid.mobile.core.utils.distanceKm
import com.solargrid.mobile.station.models.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection

class StationManager private constructor() {

    private val stationService =
        NetworkManager.getInstance().createService(StationService::class.java)

    // Stations from the last nearby search, so the detail sheet can open instantly
    private val lastNearby = mutableMapOf<String, Station>()

    // Active stations near a point, nearest first. Stations without an id or position are skipped.
    // The API measures distance from the search point; pass distanceFrom to measure from somewhere
    // else (e.g. the user while browsing another area), or null when the user's position is unknown.
    suspend fun getNearbyStations(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = DEFAULT_RADIUS_KM,
        distanceFrom: LatLng? = LatLng(latitude, longitude)
    ): Result<List<Station>> = withContext(Dispatchers.IO) {
        val response = call { stationService.getNearby(latitude, longitude, radiusKm) }
            ?: return@withContext connectionFailure()

        val body = response.body()
        if (response.isSuccessful && body != null) {
            val measuredFromCenter = distanceFrom?.latitude == latitude && distanceFrom.longitude == longitude
            val stations = body
                .filter { it.stationId != null && it.latitude != null && it.longitude != null }
                .map { station ->
                    if (measuredFromCenter) {
                        station
                    } else {
                        val position = LatLng(station.latitude!!, station.longitude!!)
                        station.copy(distanceKm = distanceFrom?.let { distanceKm(it, position) })
                    }
                }
            synchronized(lastNearby) {
                lastNearby.clear()
                stations.forEach { lastNearby[it.stationId!!] = it }
            }
            Result.success(stations)
        } else {
            httpFailure(response.code())
        }
    }

    // A station from the last nearby search, or null
    fun getCachedStation(stationId: String): Station? = synchronized(lastNearby) { lastNearby[stationId] }

    // One station by id, e.g. for the detail sheet or a booking's station name.
    // Keeps the distance from the nearby search because this endpoint has no location.
    suspend fun getStation(stationId: String): Result<Station> = withContext(Dispatchers.IO) {
        val response = call { stationService.getById(stationId) }
            ?: return@withContext connectionFailure()

        val body = response.body()
        if (response.isSuccessful && body != null) {
            val distance = body.distanceKm ?: getCachedStation(stationId)?.distanceKm
            Result.success(body.copy(distanceKm = distance))
        } else {
            httpFailure(response.code())
        }
    }

    // Run an API call; null when offline or the server can't be reached
    private suspend fun <T> call(block: suspend () -> Response<T>): Response<T>? {
        if (!NetworkManager.getInstance().isNetworkAvailable()) return null
        return try {
            block()
        } catch (e: IOException) {
            null
        } catch (e: JsonParseException) {
            null
        }
    }

    // Offline and "server down" get different messages
    private fun <T> connectionFailure(): Result<T> =
        if (NetworkManager.getInstance().isNetworkAvailable()) {
            failure(R.string.login_error_server)
        } else {
            failure(R.string.login_error_offline)
        }

    // Friendly message for an HTTP error code
    private fun <T> httpFailure(code: Int): Result<T> = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED -> failure(R.string.station_error_session)
        HttpURLConnection.HTTP_NOT_FOUND -> failure(R.string.station_error_not_found)
        else -> failure(R.string.station_error_load)
    }

    // Wrap a string resource as a failed Result
    private fun <T> failure(messageRes: Int): Result<T> {
        val message = ContextManager.getInstance().getApplicationContext().getString(messageRes)
        return Result.failure(Exception(message))
    }

    companion object {
        // Same default as the API
        const val DEFAULT_RADIUS_KM = 10.0

        // Colombo city centre, searched with a wider radius when the phone's location is unknown
        const val CITY_LATITUDE = 6.9271
        const val CITY_LONGITUDE = 79.8612
        const val CITY_RADIUS_KM = 25.0

        @Volatile
        private var instance: StationManager? = null

        // One shared StationManager for the app
        fun getInstance(): StationManager =
            instance ?: synchronized(this) {
                instance ?: StationManager().also { instance = it }
            }
    }
}
