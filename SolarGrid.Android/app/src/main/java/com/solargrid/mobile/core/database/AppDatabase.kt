/*
 * File: AppDatabase.kt
 * Description: Room database for the app. New tables are added to entities, with a step in
 *              Migrations for every version change.
 */
package com.solargrid.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.solargrid.mobile.auth.models.UserDao
import com.solargrid.mobile.auth.models.UserEntity
import com.solargrid.mobile.reservation.models.BookingDao
import com.solargrid.mobile.reservation.models.BookingEntity

@Database(entities = [UserEntity::class, BookingEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Users table queries
    abstract fun userDao(): UserDao

    // Bookings table queries (offline My Bookings)
    abstract fun bookingDao(): BookingDao
}
