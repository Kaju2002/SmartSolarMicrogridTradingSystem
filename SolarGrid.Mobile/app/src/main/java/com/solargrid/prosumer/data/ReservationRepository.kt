/*
 * File: ReservationRepository.kt
 * Description: Create, list, change and cancel reservation API calls
 */
package com.solargrid.prosumer.data

import com.google.gson.Gson
import com.solargrid.prosumer.data.api.CreateReservationRequest
import com.solargrid.prosumer.data.api.ReservationItem
import com.solargrid.prosumer.data.api.ReservationResponse
import com.solargrid.prosumer.data.api.ReservationsApi
import com.solargrid.prosumer.data.api.UpdateReservationRequest
import retrofit2.Response
import java.io.IOException

class ReservationRepository(
    private val api: ReservationsApi,
    private val sessionStore: SessionStore,
) {
    private val gson = Gson()

    suspend fun create(
        stationId: String,
        reservationDateTimeIsoUtc: String,
    ): Result<ReservationResponse> {
        val nic = sessionStore.nic?.trim().orEmpty()
        if (nic.isBlank()) {
            return Result.failure(Exception("NIC missing. Sign in again with your NIC."))
        }
        if (stationId.isBlank()) {
            return Result.failure(Exception("Select a station."))
        }

        return call("Booking failed") {
            api.create(
                CreateReservationRequest(
                    prosumerNic = nic,
                    stationId = stationId,
                    reservationDateTime = reservationDateTimeIsoUtc,
                ),
            )
        }
    }

    suspend fun reschedule(
        reservationId: String,
        newDateTimeIsoUtc: String,
    ): Result<ReservationResponse> {
        if (reservationId.isBlank()) {
            return Result.failure(Exception("Select a booking."))
        }
        return call("Could not change booking") {
            api.update(reservationId, UpdateReservationRequest(newDateTimeIsoUtc))
        }
    }

    suspend fun cancel(reservationId: String): Result<ReservationResponse> {
        if (reservationId.isBlank()) {
            return Result.failure(Exception("Select a booking."))
        }
        return call("Could not cancel booking") {
            api.cancel(reservationId)
        }
    }

    suspend fun listMine(): Result<List<ReservationItem>> {
        val nic = sessionStore.nic?.trim().orEmpty()
        if (nic.isBlank()) {
            return Result.failure(Exception("NIC missing. Sign in again with your NIC."))
        }

        return try {
            val http = api.listByProsumer(nic)
            if (!http.isSuccessful) {
                return Result.failure(Exception(http.errorMessage("Could not load bookings")))
            }
            Result.success(http.body().orEmpty())
        } catch (_: IOException) {
            Result.failure(Exception(NETWORK_ERROR))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun call(
        fallback: String,
        request: suspend () -> Response<ReservationResponse>,
    ): Result<ReservationResponse> {
        return try {
            val http = request()
            if (!http.isSuccessful) {
                return Result.failure(Exception(http.errorMessage(fallback)))
            }
            val body = http.body()
            if (body == null || !body.success) {
                return Result.failure(Exception(body?.message?.ifBlank { null } ?: fallback))
            }
            Result.success(body)
        } catch (_: IOException) {
            Result.failure(Exception(NETWORK_ERROR))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Non-2xx bodies land in errorBody(), not body(). */
    private fun Response<*>.errorMessage(fallback: String): String {
        val serverMessage = runCatching {
            errorBody()?.string()?.let { gson.fromJson(it, ReservationResponse::class.java) }
        }.getOrNull()?.message?.takeIf { it.isNotBlank() }

        return serverMessage ?: when (code()) {
            401 -> "Session expired. Sign in again."
            403 -> "Only Prosumer accounts can manage bookings."
            404 -> "Booking not found."
            else -> "$fallback (${code()})"
        }
    }

    private companion object {
        const val NETWORK_ERROR =
            "Network error. Is the API running? Emulator uses http://10.0.2.2:5204"
    }
}
