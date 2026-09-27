/*
 * File: DatabaseManager.kt
 * Description: Opens the Room database once and hands out the DAOs
 */
package com.solargrid.mobile.core.managers

import androidx.room.Room
import com.solargrid.mobile.auth.models.UserDao
import com.solargrid.mobile.core.database.AppDatabase

class DatabaseManager private constructor() {

    // Local cache only, so on a version change we just rebuild the tables
    private val database: AppDatabase = Room.databaseBuilder(
        ContextManager.getInstance().getApplicationContext(),
        AppDatabase::class.java,
        DATABASE_NAME
    ).fallbackToDestructiveMigration(true).build()

    // Users table
    fun userDao(): UserDao = database.userDao()

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
