/*
 * File: UserEntity.kt
 * Module: Identity and Access (Vithusha)
 * Description: Logged-in user saved in SQLite (Room). No password is stored.
 */
package com.solargrid.mobile.auth.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    // "Prosumer" or "GridOperator"
    val userType: String,
    // Prosumers log in with NIC, operators with username - only one is set
    val nic: String?,
    val username: String?,
    // JWT, sent again after the app restarts
    val token: String,
    // Epoch millis, used to drop the session after the JWT expires
    val loggedInAt: Long
)
