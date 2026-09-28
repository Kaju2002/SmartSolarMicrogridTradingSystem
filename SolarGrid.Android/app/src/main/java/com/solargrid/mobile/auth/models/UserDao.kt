/*
 * File: UserDao.kt
 * Module: Identity and Access (Vithusha)
 * Description: SQL queries for the users table
 */
package com.solargrid.mobile.auth.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    // Save the user after login (replaces the same userId)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    // Only one user is logged in at a time
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    // Keep the saved name in step after a profile edit
    @Query("UPDATE users SET fullName = :fullName WHERE userId = :userId")
    suspend fun updateFullName(userId: String, fullName: String)

    // Logout
    @Query("DELETE FROM users")
    suspend fun clear()
}
