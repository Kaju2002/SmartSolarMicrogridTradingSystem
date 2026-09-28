/*
 * File: BookingDao.kt
 * Module: Reservation Management (Kajanthan)
 * Description: SQL queries for the bookings table
 */
package com.solargrid.mobile.reservation.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface BookingDao {

    // Saved bookings of one prosumer
    @Query("SELECT * FROM bookings WHERE ownerNic = :nic")
    suspend fun getForOwner(nic: String): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookings: List<BookingEntity>)

    @Query("DELETE FROM bookings WHERE ownerNic = :nic")
    suspend fun deleteForOwner(nic: String)

    // Swap in the latest list in one go, so cancelled/removed bookings don't linger
    @Transaction
    suspend fun replaceForOwner(nic: String, bookings: List<BookingEntity>) {
        deleteForOwner(nic)
        insertAll(bookings)
    }

    // Logout
    @Query("DELETE FROM bookings")
    suspend fun clear()
}
