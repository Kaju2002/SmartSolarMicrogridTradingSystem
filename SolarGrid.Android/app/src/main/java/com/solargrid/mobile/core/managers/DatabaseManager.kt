/*
 * File: DatabaseManager.kt
 * Description: Opens the Room database once and hands out the DAOs
 */
package com.solargrid.mobile.core.managers

import androidx.room.Room
import com.solargrid.mobile.auth.models.UserDao
import com.solargrid.mobile.core.database.AppDatabase
import com.solargrid.mobile.core.database.Migrations
import com.solargrid.mobile.reservation.models.BookingDao

class DatabaseManager private constructor() {

    // Known version steps keep the data; anything else is a local cache we can rebuild
    private val database: AppDatabase = Room.databaseBuilder(
        ContextManager.getInstance().getApplicationContext(),
        AppDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(*Migrations.ALL).fallbackToDestructiveMigration(true).build()

    // Users table
    fun userDao(): UserDao = database.userDao()

    // Bookings table
    fun bookingDao(): BookingDao = database.bookingDao()

    companion object {
        private const val DATABASE_NAME = "solargrid.db"

        @Volatile
        private var instance: DatabaseManager? = null

        // One database connection for the whole app
        fun getInstance(): DatabaseManager =
            instance ?: synchronized(this) {
                instance ?: DatabaseManager().also { instance = it }
            }
    }
}
