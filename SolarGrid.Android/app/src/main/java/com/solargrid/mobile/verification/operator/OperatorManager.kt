/*
 * File: OperatorManager.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator data: bookings and stations for the signed-in operator,
 *              approve and QR scan. Rules live in the API; its messages are shown as they are.
 *              Needs the API; nothing here is saved on the phone.
 */
package com.solargrid.mobile.verification.operator

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.DatabaseManager
import com.solargrid.mobile.core.managers.NetworkManager
import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import com.solargrid.mobile.verification.operator.models.OperatorStation
import com.solargrid.mobile.verification.operator.models.ScanQrRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection

class OperatorManager private constructor() {

    private val operatorService =
        NetworkManager.getInstance().createService(OperatorService::class.java)
    private val gson = Gson()

    // Bookings at the operator's stations, soonest slot first (the API picks them from the token)
    suspend fun getBookings(): Result<List<OperatorBooking>> = withContext(Dispatchers.IO) {
        val response = call { operatorService.getBookings() }
            ?: return@withContext connectionFailure()

        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body.filter { it.id != null })
        } else {
            httpFailure(response, R.string.operator_error_bookings)
        }
    }

    // Stations assigned to the signed-in operator, by name
    suspend fun getMyStations(): Result<List<OperatorStation>> = withContext(Dispatchers.IO) {
        val userId = DatabaseManager.getInstance().userDao().getCurrentUser()?.userId
            ?: return@withContext failure(R.string.booking_error_session)

        val response = call { operatorService.getStations() }
            ?: return@withContext connectionFailure()

        val body = response.body()
        if (response.isSuccessful && body != null) {
            val mine = body.filter { it.id != null && it.assignedOperatorId == userId }
            Result.success(mine.sortedBy { it.stationName.orEmpty().lowercase() })
        } else {
            httpFailure(response, R.string.operator_error_stations)
        }
    }

    // Approve a Pending booking; the API creates the QR code
    suspend fun approve(reservationId: String): Result<ReservationResult> =
        action(R.string.operator_error_approve) { operatorService.approve(reservationId) }

    // Complete the energy transfer for a scanned or typed QR code
    suspend fun scanQr(qrCode: String): Result<ReservationResult> =
        action(R.string.operator_error_scan) { operatorService.scanQr(ScanQrRequest(qrCode.trim())) }

    // Approve and scan share the same success and error handling
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
            HttpURLConnection.HTTP_FORBIDDEN -> failure(R.string.operator_error_forbidden)
            else -> apiFailure(readErrorMessage(response), fallbackRes)
        }

    // API message if there is one, else the fallback text
    private fun <T> apiFailure(message: String?, fallbackRes: Int): Result<T> =
        if (message.isNullOrBlank()) failure(fallbackRes) else Result.failure(Exception(message))

    // 400 bodies have the ReservationResult shape, e.g. "This QR code has already been used"
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
        private var instance: OperatorManager? = null

        // One shared OperatorManager for the app
        fun getInstance(): OperatorManager =
            instance ?: synchronized(this) {
                instance ?: OperatorManager().also { instance = it }
            }
    }
}
