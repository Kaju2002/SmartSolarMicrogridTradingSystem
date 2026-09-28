/*
 * File: ReservationManager.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Slot availability, booking, change, cancel and the prosumer's booking list.
 *              Booking rules live in the API; its messages are shown to the user as they are.
 *              The booking list is saved in SQLite after each online load and read back when
 *              the phone is offline or the server is down.
 */
package com.solargrid.mobile.reservation

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.DatabaseManager
import com.solargrid.mobile.core.managers.NetworkManager
import com.solargrid.mobile.reservation.models.BookingEntity
import com.solargrid.mobile.reservation.models.BookingList
import com.solargrid.mobile.reservation.models.CreateReservationRequest
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.reservation.models.SlotAvailability
import com.solargrid.mobile.reservation.models.UpdateReservationRequest
import com.solargrid.mobile.station.StationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap

class ReservationManager private constructor() {

    private val reservationService =
        NetworkManager.getInstance().createService(ReservationService::class.java)
    private val gson = Gson()
    private val bookingDao get() = DatabaseManager.getInstance().bookingDao()

    // Station id -> name, looked up once per station while the app runs
    private val stationNames = ConcurrentHashMap<String, String>()

    // Hourly slots for a station on one day ("yyyy-MM-dd", Sri Lanka). Slots without a time are skipped.
    suspend fun getAvailability(stationId: String, date: String): Result<SlotAvailability> =
        withContext(Dispatchers.IO) {
            val response = call { reservationService.getAvailability(stationId, date) }
                ?: return@withContext connectionFailure()

            val body = response.body()
            if (response.isSuccessful && body != null) {
                val slots = body.slots.orEmpty().filter { it.slotDateTime != null }
                Result.success(body.copy(slots = slots))
            } else {
                httpFailure(response, R.string.booking_error_slots)
            }
        }

    // Book a slot; slotDateTime is sent exactly as the availability call returned it.
    // The API checks the kWh against what is left and works out the cost.
    suspend fun createReservation(
        stationId: String,
        slotDateTime: String,
        requestedKWh: Double
    ): Result<ReservationResult> =
        action(R.string.booking_error_book) {
            reservationService.create(CreateReservationRequest(stationId, slotDateTime, requestedKWh))
        }

    // Move a booking to another slot (API allows it until 12 hours before)
    suspend fun updateReservation(reservationId: String, newSlotDateTime: String): Result<ReservationResult> =
        action(R.string.booking_error_update) {
            reservationService.update(reservationId, UpdateReservationRequest(newSlotDateTime))
        }

    // Cancel a booking (API allows it until 12 hours before)
    suspend fun cancelReservation(reservationId: String): Result<ReservationResult> =
        action(R.string.booking_error_cancel) { reservationService.cancel(reservationId) }

    // The signed-in prosumer's bookings, earliest slot first. Online results are saved;
    // offline or server errors fall back to the saved copy when there is one.
    suspend fun getMyBookings(): Result<BookingList> = withContext(Dispatchers.IO) {
        val nic = DatabaseManager.getInstance().userDao().getCurrentUser()?.nic
            ?: return@withContext failure(R.string.booking_error_session)

        val response = call { reservationService.getByProsumer(nic) }
            ?: return@withContext savedBookings(nic) ?: connectionFailure()

        val body = response.body()
        when {
            response.isSuccessful && body != null -> {
                val bookings = sortBySlot(body.filter { it.id != null })
                val names = stationNamesFor(nic, bookings)
                val savedAt = System.currentTimeMillis()
                bookingDao.replaceForOwner(nic, bookings.mapNotNull { booking ->
                    BookingEntity.from(booking, nic, booking.stationId?.let { names[it] }, savedAt)
                })
                Result.success(BookingList(bookings, names, offline = false, savedAt = savedAt))
            }
            response.code() >= HttpURLConnection.HTTP_INTERNAL_ERROR ->
                savedBookings(nic) ?: httpFailure(response, R.string.booking_error_list)
            else -> httpFailure(response, R.string.booking_error_list)
        }
    }

    // The copy saved at the last online load, or null if there is none
    private suspend fun savedBookings(nic: String): Result<BookingList>? {
        val rows = bookingDao.getForOwner(nic)
        if (rows.isEmpty()) return null

        val names = rows.mapNotNull { row -> row.stationName?.let { name -> row.stationId?.let { it to name } } }.toMap()
        val list = BookingList(
            bookings = sortBySlot(rows.map { it.toReservation() }),
            stationNames = names,
            offline = true,
            savedAt = rows.maxOf { it.savedAt }
        )
        return Result.success(list)
    }

    private fun sortBySlot(bookings: List<Reservation>): List<Reservation> =
        bookings.sortedBy { ReservationTime.parseUtc(it.reservationDateTime) ?: Long.MAX_VALUE }

    // Names from memory, the saved copy, the map's cache, then one API call per new station
    private suspend fun stationNamesFor(nic: String, bookings: List<Reservation>): Map<String, String> {
        val ids = bookings.mapNotNull { it.stationId }.distinct()
        if (ids.any { !stationNames.containsKey(it) }) {
            bookingDao.getForOwner(nic).forEach { row ->
                val id = row.stationId
                val name = row.stationName
                if (id != null && name != null) stationNames.putIfAbsent(id, name)
            }
        }

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

        return ids.mapNotNull { id -> stationNames[id]?.let { id to it } }.toMap()
    }

    // Book / change / cancel share the same success and error handling
    private suspend fun action(
        fallbackRes: Int,
        block: suspend () -> Response<ReservationResult>
    ): Result<ReservationResult> = withContext(Dispatchers.IO) {
        val response = call(block) ?: return@withContext connectionFailure()

        val body = response.body()
        when {
            response.isSuccessful && body?.success == true -> Result.success(body)
            response.isSuccessful -> apiFailure(body?.message, fallbackRes)
            else -> httpFailure(response, fallbackRes)
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

    // 401/403 are account problems; otherwise use the API's rule message when it sent one
    private fun <T> httpFailure(response: Response<*>, fallbackRes: Int): Result<T> =
        when (response.code()) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> failure(R.string.booking_error_session)
            HttpURLConnection.HTTP_FORBIDDEN -> failure(R.string.booking_error_forbidden)
            else -> apiFailure(readErrorMessage(response), fallbackRes)
        }

    // API message if there is one, else the fallback text
    private fun <T> apiFailure(message: String?, fallbackRes: Int): Result<T> =
        if (message.isNullOrBlank()) failure(fallbackRes) else Result.failure(Exception(message))

    // 400/404 bodies have the ReservationResult shape, e.g. "This time slot is full"
    private fun readErrorMessage(response: Response<*>): String? {
        val json = try {
            response.errorBody()?.string()
        } catch (e: IOException) {
            null
        } ?: return null

        return try {
            gson.fromJson(json, ReservationResult::class.java)?.message
        } catch (e: JsonParseException) {
            null
        }
    }

    // Wrap a string resource as a failed Result
    private fun <T> failure(messageRes: Int): Result<T> {
        val message = ContextManager.getInstance().getApplicationContext().getString(messageRes)
        return Result.failure(Exception(message))
    }

    companion object {
        @Volatile
        private var instance: ReservationManager? = null

        // One shared ReservationManager for the app
        fun getInstance(): ReservationManager =
            instance ?: synchronized(this) {
                instance ?: ReservationManager().also { instance = it }
            }
    }
}
