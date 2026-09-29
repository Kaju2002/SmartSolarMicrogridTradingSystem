/*
 * File: DashboardManager.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Loads the prosumer dashboard: booking counts, completed history and
 *              bookings by status. Needs the API; nothing here is saved on the phone.
 */
package com.solargrid.mobile.verification.dashboard

import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.DatabaseManager
import com.solargrid.mobile.core.managers.NetworkManager
import com.solargrid.mobile.reservation.ReservationTime
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.station.StationManager
import com.solargrid.mobile.verification.dashboard.models.DashboardSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap

class DashboardManager private constructor() {

    private val dashboardService =
        NetworkManager.getInstance().createService(DashboardService::class.java)

    // Station id -> name, looked up once per station while the app runs
    private val stationNames = ConcurrentHashMap<String, String>()

    // Pending, approved and completed counts for the signed-in prosumer
    suspend fun getSummary(): Result<DashboardSummary> = load { nic -> dashboardService.getSummary(nic) }

    // Completed bookings, newest first
    suspend fun getHistory(): Result<List<Reservation>> =
        load { nic -> dashboardService.getHistory(nic) }.map { newestFirst(it) }

    // Bookings with one status ("Pending", "Approved", ...); null status returns every booking
    suspend fun searchByStatus(status: String?): Result<List<Reservation>> =
        load { nic -> dashboardService.search(nic, status) }.map { newestFirst(it) }

    // Names from memory, the map's cache, then one API call per new station
    suspend fun stationNamesFor(bookings: List<Reservation>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val ids = bookings.mapNotNull { it.stationId }.distinct()
            val stationManager = StationManager.getInstance()
            val found = coroutineScope {
                ids.filter { !stationNames.containsKey(it) }.map { id ->
                    async {
                        val name = stationManager.getCachedStation(id)?.stationName
                            ?: stationManager.getStation(id).getOrNull()?.stationName
                        id to name
                    }
                }.awaitAll()
            }
            found.forEach { (id, name) -> if (!name.isNullOrBlank()) stationNames[id] = name }
            ids.mapNotNull { id -> stationNames[id]?.let { id to it } }.toMap()
        }

    // Latest slot on top; bookings without a readable time go last
    private fun newestFirst(bookings: List<Reservation>): List<Reservation> =
        bookings.filter { it.id != null }
            .sortedByDescending { ReservationTime.parseUtc(it.reservationDateTime) ?: Long.MIN_VALUE }

    // Shared steps for every call: saved NIC, network check, then the error handling
    private suspend fun <T> load(block: suspend (nic: String) -> Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            val nic = DatabaseManager.getInstance().userDao().getCurrentUser()?.nic
                ?: return@withContext failure(R.string.booking_error_session)
            if (!NetworkManager.getInstance().isNetworkAvailable()) {
                return@withContext failure(R.string.login_error_offline)
            }

            val response = try {
                block(nic)
            } catch (e: IOException) {
                return@withContext failure(R.string.login_error_server)
            } catch (e: JsonParseException) {
                return@withContext failure(R.string.login_error_server)
            }

            val body = response.body()
            when {
                response.isSuccessful && body != null -> Result.success(body)
                response.code() == HttpURLConnection.HTTP_UNAUTHORIZED -> failure(R.string.booking_error_session)
                response.code() == HttpURLConnection.HTTP_FORBIDDEN -> failure(R.string.dashboard_error_forbidden)
                else -> failure(R.string.dashboard_error_load)
            }
        }

    // Wrap a string resource as a failed Result
    private fun <T> failure(messageRes: Int): Result<T> {
        val message = ContextManager.getInstance().getApplicationContext().getString(messageRes)
        return Result.failure(Exception(message))
    }

    companion object {
        @Volatile
        private var instance: DashboardManager? = null

        // One shared DashboardManager for the app
        fun getInstance(): DashboardManager =
            instance ?: synchronized(this) {
                instance ?: DashboardManager().also { instance = it }
            }
    }
}
