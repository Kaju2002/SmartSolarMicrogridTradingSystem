/*
 * File: AppDatabase.kt
 * Description: Room database for the app. New tables are added to entities.
 */
package com.solargrid.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.solargrid.mobile.auth.models.UserDao
import com.solargrid.mobile.auth.models.UserEntity

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Users table queries
    abstract fun userDao(): UserDao
}
