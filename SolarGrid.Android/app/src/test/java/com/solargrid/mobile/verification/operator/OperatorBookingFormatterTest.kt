/*
 * File: OperatorBookingFormatterTest.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Checks the operator booking groups use Sri Lanka days, skip slots that are over,
 *              and only allow approving live Pending bookings
 */
package com.solargrid.mobile.verification.operator

import com.solargrid.mobile.reservation.ReservationTime
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorBookingFormatterTest {

    // 30 Sep 2026, 13:30 in Sri Lanka
    private val now = ReservationTime.parseUtc("2026-09-30T08:00:00Z")!!

    private val bookings = listOf(
        booking("pending-tomorrow", Reservation.STATUS_PENDING, "2026-10-01T04:30:00Z"),
        booking("pending-over", Reservation.STATUS_PENDING, "2026-09-30T06:00:00Z"),
        booking("approved-later", Reservation.STATUS_APPROVED, "2026-09-30T10:30:00Z"),
        booking("approved-soon", Reservation.STATUS_APPROVED, "2026-09-30T09:30:00Z"),
        booking("approved-over", Reservation.STATUS_APPROVED, "2026-09-30T05:00:00Z"),
        // 00:30 on 1 Oct in Sri Lanka, though still 30 Sep in UTC
        booking("approved-after-midnight", Reservation.STATUS_APPROVED, "2026-09-30T19:00:00Z"),
        booking("completed-today", Reservation.STATUS_COMPLETED, "2026-09-30T03:30:00Z"),
        booking("completed-yesterday", Reservation.STATUS_COMPLETED, "2026-09-29T03:30:00Z"),
        booking("cancelled-today", Reservation.STATUS_CANCELLED, "2026-09-30T10:30:00Z")
    )

    // Pending that can still be approved, any day
    @Test
    fun pendingSkipsSlotsThatAreOver() {
        val ids = OperatorBookingFormatter.pending(bookings, now).map { it.id }
        assertEquals(listOf("pending-tomorrow"), ids)
    }

    // Today in Sri Lanka only, not over yet, soonest first
    @Test
    fun approvedTodayIsSriLankaDaySoonestFirst() {
        val ids = OperatorBookingFormatter.approvedToday(bookings, now).map { it.id }
        assertEquals(listOf("approved-soon", "approved-later"), ids)
    }

    // Completed on today's slots only
    @Test
    fun completedTodayOnlyCountsToday() {
        val ids = OperatorBookingFormatter.completedToday(bookings, now).map { it.id }
        assertEquals(listOf("completed-today"), ids)
    }

    // Approved on any day, not over yet, soonest first
    @Test
    fun approvedUpcomingIncludesLaterDays() {
        val ids = OperatorBookingFormatter.approvedUpcoming(bookings, now).map { it.id }
        assertEquals(listOf("approved-soon", "approved-later", "approved-after-midnight"), ids)
    }

    // Every completed transfer, latest first
    @Test
    fun completedIsLatestFirst() {
        val ids = OperatorBookingFormatter.completed(bookings).map { it.id }
        assertEquals(listOf("completed-today", "completed-yesterday"), ids)
    }

    // Approve only while Pending and before the slot ends
    @Test
    fun canApproveOnlyLivePending() {
        val byId = bookings.associateBy { it.id }
        assertTrue(OperatorBookingFormatter.canApprove(byId.getValue("pending-tomorrow"), now))
        assertFalse(OperatorBookingFormatter.canApprove(byId.getValue("pending-over"), now))
        assertFalse(OperatorBookingFormatter.canApprove(byId.getValue("approved-soon"), now))
    }

    private fun booking(id: String, status: String, time: String) = OperatorBooking(
        id = id,
        prosumerNic = "200167397996",
        stationId = "st1",
        stationName = "Malabe Solar Hub",
        reservationDateTime = time,
        requestedKWh = 10.0,
        estimatedCost = 450.0,
        status = status,
        qrCode = null,
        createdAt = null,
        lastModifiedAt = null
    )
}
