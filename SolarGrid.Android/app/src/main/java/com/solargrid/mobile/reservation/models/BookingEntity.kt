/*
 * File: BookingEntity.kt
 * Module: Reservation Management (Kajanthan)
 * Description: One of the prosumer's bookings saved in SQLite (Room), so My Bookings and the QR
 *              code still work with no signal at the station. Replaced on every online load.
 */
package com.solargrid.mobile.reservation.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookings", indices = [Index("ownerNic")])
data class BookingEntity(
    @PrimaryKey val id: String,
    // NIC of the prosumer who made the booking
    val ownerNic: String,
    val stationId: String?,
    // Saved so the card shows the real name offline
    val stationName: String?,
    val reservationDateTime: String?,
    val requestedKWh: Double?,
    val estimatedCost: Double?,
    val status: String?,
    val qrCode: String?,
    val createdAt: String?,
    // Epoch millis of the online load that saved this row
    val savedAt: Long
) {

    // Back to the model the screens use
    fun toReservation(): Reservation = Reservation(
        id, stationId, reservationDateTime, requestedKWh, estimatedCost, status, qrCode, createdAt
    )

    companion object {
        // Row for one booking; null if the API sent it without an id
        fun from(booking: Reservation, ownerNic: String, stationName: String?, savedAt: Long): BookingEntity? {
            val id = booking.id ?: return null
            return BookingEntity(
                id = id,
                ownerNic = ownerNic,
                stationId = booking.stationId,
                stationName = stationName,
                reservationDateTime = booking.reservationDateTime,
                requestedKWh = booking.requestedKWh,
                estimatedCost = booking.estimatedCost,
                status = booking.status,
                qrCode = booking.qrCode,
                createdAt = booking.createdAt,
                savedAt = savedAt
            )
        }
    }
}
