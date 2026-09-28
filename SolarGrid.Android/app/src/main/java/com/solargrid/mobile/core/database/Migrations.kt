/*
 * File: Migrations.kt
 * Description: Room schema changes. Each step keeps the existing tables, so the logged-in
 *              user is not signed out when the app updates.
 */
package com.solargrid.mobile.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // Version 2 adds the bookings table (offline My Bookings and QR)
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `bookings` (" +
                    "`id` TEXT NOT NULL, `ownerNic` TEXT NOT NULL, `stationId` TEXT, `stationName` TEXT, " +
                    "`reservationDateTime` TEXT, `requestedKWh` REAL, `estimatedCost` REAL, `status` TEXT, " +
                    "`qrCode` TEXT, `createdAt` TEXT, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookings_ownerNic` ON `bookings` (`ownerNic`)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
