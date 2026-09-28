/*
 * File: ReservationModelsTest.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Checks the booking models read the API's JSON and the time helpers
 */
package com.solargrid.mobile.reservation

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.solargrid.mobile.reservation.models.BookingEntity
import com.solargrid.mobile.reservation.models.BookingSlot
import com.solargrid.mobile.reservation.models.CreateReservationRequest
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.reservation.models.SlotAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationModelsTest {

    private val gson = Gson()

    // Availability JSON as SlotAvailabilityDto sends it
    @Test
    fun readsAvailability() {
        val json = """
            {"stationId":"66f0a1","date":"2026-09-29","openTime":"06:00","closeTime":"20:00","spotsPerSlot":15,
             "capacityKWh":120,"ratePerKwh":45,"minKWh":1,
             "slots":[
               {"slotDateTime":"2026-09-29T00:30:00Z","startTime":"06:00","endTime":"07:00","spotsLeft":15,"kWhLeft":110.5,"status":"Available"},
               {"slotDateTime":"2026-09-29T01:30:00Z","startTime":"07:00","endTime":"08:00","spotsLeft":0,"kWhLeft":0,"status":"Full"}]}
        """.trimIndent()

        val availability = gson.fromJson(json, SlotAvailability::class.java)

        assertEquals(15, availability.spotsPerSlot)
        assertEquals(45.0, availability.ratePerKwh!!, 0.0)
        assertEquals(110.5, availability.slots!![0].kWhLeft!!, 0.0)
        assertEquals(2, availability.slots?.size)
        assertEquals("06:00", availability.slots?.first()?.startTime)
        assertTrue(availability.slots!![0].isAvailable())
        assertFalse(availability.slots!![1].isAvailable())
    }

    // Booking list JSON as EnergyReservation sends it
    @Test
    fun readsReservationList() {
        val json = """
            [{"id":"r1","prosumerNic":"200012345678","stationId":"66f0a1","bookingSlotId":"s1",
              "reservationDateTime":"2026-09-29T03:30:00Z","requestedKWh":10,"estimatedCost":450,
              "status":"Approved","qrCode":"abc",
              "approvedBy":"u9","createdAt":"2026-09-28T01:02:03.456Z","lastModifiedAt":null}]
        """.trimIndent()
        val type = object : TypeToken<List<Reservation>>() {}.type

        val list: List<Reservation> = gson.fromJson(json, type)

        assertEquals("r1", list[0].id)
        assertEquals("abc", list[0].qrCode)
        assertEquals(450.0, list[0].estimatedCost!!, 0.0)
        assertTrue(list[0].isLive())
    }

    // 400 body carries the API rule message
    @Test
    fun readsErrorMessage() {
        val result = gson.fromJson("""{"success":false,"message":"This time slot is full"}""", ReservationResult::class.java)

        assertFalse(result.success)
        assertEquals("This time slot is full", result.message)
    }

    // Request keys must match CreateReservationDto
    @Test
    fun writesCreateRequest() {
        val request = CreateReservationRequest("66f0a1", "2026-09-29T00:30:00Z", 12.0)
        val body = JsonParser.parseString(gson.toJson(request)).asJsonObject

        assertEquals("66f0a1", body.get("stationId").asString)
        assertEquals("2026-09-29T00:30:00Z", body.get("reservationDateTime").asString)
        assertEquals(12.0, body.get("requestedKWh").asDouble, 0.0)
    }

    // UTC text with or without fractions parses to the same instant
    @Test
    fun parsesUtcTimes() {
        val plain = ReservationTime.parseUtc("2026-09-29T03:30:00Z")

        assertEquals(1790652600000L, plain)
        assertEquals(plain, ReservationTime.parseUtc("2026-09-29T03:30:00.000Z"))
        assertNull(ReservationTime.parseUtc("not a date"))
        assertNull(ReservationTime.parseUtc(null))
    }

    // 20:00 UTC is already the next day in Sri Lanka (01:30)
    @Test
    fun buildsSriLankaDate() {
        val lateUtc = ReservationTime.parseUtc("2026-09-28T20:00:00Z")!!

        assertEquals("2026-09-29", ReservationTime.apiDate(lateUtc))
    }

    // Seven day chips, one Sri Lanka day apart, starting today
    @Test
    fun buildsSevenBookableDays() {
        val days = ReservationTime.bookableDays()

        assertEquals(ReservationTime.BOOKABLE_DAYS, days.size)
        assertEquals(ReservationTime.apiDate(System.currentTimeMillis()), ReservationTime.apiDate(days[0]))
        assertEquals(24 * 60 * 60 * 1000L, days[1] - days[0])
        assertEquals(7, days.map { ReservationTime.apiDate(it) }.distinct().size)
    }

    // Unknown slot status is never bookable
    @Test
    fun slotWithoutTimeIsNotBookable() {
        assertFalse(BookingSlot(null, "06:00", "07:00", 5, 40.0, BookingSlot.STATUS_AVAILABLE).isAvailable())
    }

    // 03:30 UTC is 09:00 in Sri Lanka
    @Test
    fun showsSriLankaClock() {
        val start = ReservationTime.parseUtc("2026-09-29T03:30:00Z")!!

        assertEquals("09:00", ReservationTime.clock(start))
        assertEquals("10:00", ReservationTime.clock(start + ReservationTime.SLOT_LENGTH_MS))
    }

    // A booking counts as past only after its one-hour slot ends
    @Test
    fun bookingIsPastAfterSlotEnds() {
        val booking = Reservation("r1", "s1", "2026-09-29T03:30:00Z", 10.0, 450.0, Reservation.STATUS_APPROVED, null, null)
        val start = ReservationTime.parseUtc(booking.reservationDateTime)!!

        assertFalse(ReservationFormatter.isPast(booking, now = start + 30 * 60 * 1000L))
        assertTrue(ReservationFormatter.isPast(booking, now = start + ReservationTime.SLOT_LENGTH_MS))
    }

    // Change / cancel stays open until exactly 12 hours before the slot, like the API
    @Test
    fun changeClosesTwelveHoursBefore() {
        val booking = Reservation("r1", "s1", "2026-09-29T03:30:00Z", 10.0, 450.0, Reservation.STATUS_PENDING, null, null)
        val deadline = ReservationFormatter.changeDeadline(booking)!!

        assertEquals(ReservationTime.parseUtc(booking.reservationDateTime)!! - 12 * 60 * 60 * 1000L, deadline)
        assertTrue(ReservationFormatter.canChange(booking, now = deadline))
        assertFalse(ReservationFormatter.canChange(booking, now = deadline + 1))
    }

    // A saved row gives back the same booking, QR code included, for offline use
    @Test
    fun savedBookingRoundTrip() {
        val booking = Reservation("r1", "s1", "2026-09-29T03:30:00Z", 10.0, 450.0, Reservation.STATUS_APPROVED, "qr-123", "2026-09-28T01:00:00Z")
        val row = BookingEntity.from(booking, "200012345678", "Colombo Fort Solar Hub", savedAt = 1000L)!!

        assertEquals("200012345678", row.ownerNic)
        assertEquals("Colombo Fort Solar Hub", row.stationName)
        assertEquals(booking, row.toReservation())
    }

    // Bookings without an id can't be saved (id is the primary key)
    @Test
    fun bookingWithoutIdIsNotSaved() {
        val booking = Reservation(null, "s1", "2026-09-29T03:30:00Z", 10.0, 450.0, Reservation.STATUS_PENDING, null, null)

        assertNull(BookingEntity.from(booking, "200012345678", null, savedAt = 1000L))
    }

    // Cancelled and completed bookings can't be changed, however far away
    @Test
    fun onlyLiveBookingsCanChange() {
        val cancelled = Reservation("r1", "s1", "2026-09-29T03:30:00Z", 10.0, 450.0, Reservation.STATUS_CANCELLED, null, null)
        val farAhead = ReservationTime.parseUtc(cancelled.reservationDateTime)!! - 3 * 24 * 60 * 60 * 1000L

        assertFalse(ReservationFormatter.canChange(cancelled, now = farAhead))
    }
}
