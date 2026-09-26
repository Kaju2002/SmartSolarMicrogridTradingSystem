/*
 * File: BookingDraftStore.kt
 * Description: Holds station selected from Map for Book tab
 */
package com.solargrid.prosumer.data

class BookingDraftStore {
    var stationId: String? = null
    var stationName: String? = null

    fun setStation(id: String, name: String) {
        stationId = id
        stationName = name
    }

    fun clear() {
        stationId = null
        stationName = null
    }
}
