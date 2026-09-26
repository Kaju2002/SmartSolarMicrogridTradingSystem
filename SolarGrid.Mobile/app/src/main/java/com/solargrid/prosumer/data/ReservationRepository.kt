/*
 * File: ReservationRepository.kt
 * Description: Create and list reservation API calls
 */
package com.solargrid.prosumer.data

import com.solargrid.prosumer.data.api.CreateReservationRequest
import com.solargrid.prosumer.data.api.ReservationItem
import com.solargrid.prosumer.data.api.ReservationResponse
import com.solargrid.prosumer.data.api.ReservationsApi
import java.io.IOException

class ReservationRepository(
    private val api: ReservationsApi,
    private val sessionStore: SessionStore,
) {
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

        return try {
            val http = api.create(
                CreateReservationRequest(
                    prosumerNic = nic,
                    stationId = stationId,
                    reservationDateTime = reservationDateTimeIsoUtc,
                ),
            )
            val body = http.body()
            if (!http.isSuccessful) {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: when (http.code()) {
                        401 -> "Session expired. Sign in again."
                        403 -> "Only Prosumer can create bookings."
                        else -> "Booking failed (${http.code()})"
                    }
                return Result.failure(Exception(msg))
            }
            if (body == null || !body.success) {
                return Result.failure(
                    Exception(body?.message?.ifBlank { null } ?: "Booking failed"),
                )
            }
            Result.success(body)
        } catch (_: IOException) {
            Result.failure(
                Exception("Network error. Is the API running? Emulator uses http://10.0.2.2:5204"),
            )
        } catch (e: Exception) {
            Result.failure(e)
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
                return Result.failure(
                    Exception(
                        when (http.code()) {
                            401 -> "Session expired. Sign in again."
                            403 -> "Not allowed to view bookings."
                            else -> "Could not load bookings (${http.code()})"
                        },
                    ),
                )
            }
            Result.success(http.body().orEmpty())
        } catch (_: IOException) {
            Result.failure(
                Exception("Network error. Is the API running? Emulator uses http://10.0.2.2:5204"),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
