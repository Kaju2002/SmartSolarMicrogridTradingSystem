/*
 * File: DashboardRepository.kt
 * Description: Prosumer dashboard API calls
 */
package com.solargrid.prosumer.data

import com.solargrid.prosumer.data.api.DashboardApi
import com.solargrid.prosumer.data.api.DashboardSummary
import java.io.IOException

class DashboardRepository(
    private val api: DashboardApi,
    private val sessionStore: SessionStore,
) {
    suspend fun getSummary(): Result<DashboardSummary> {
        val nic = sessionStore.nic?.trim().orEmpty()
        if (nic.isBlank()) {
            return Result.failure(Exception("NIC missing. Sign in again with your NIC."))
        }

        return try {
            val http = api.getSummary(nic)
            val body = http.body()
            if (!http.isSuccessful || body == null) {
                return Result.failure(
                    Exception(
                        when (http.code()) {
                            401 -> "Session expired. Sign in again."
                            403 -> "Not allowed to view dashboard."
                            else -> "Could not load summary (${http.code()})"
                        },
                    ),
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
}
