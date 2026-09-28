/*
 * File: BookSlotActivity.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Booking screen opened from a station. The user picks a day (next 7 days), a free
 *              hourly slot and how many kWh, then confirms. Slot states, kWh limits, booking rules
 *              and the final cost come from the API; the cost shown before booking is a preview.
 */
package com.solargrid.mobile.reservation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityBookSlotBinding
import com.solargrid.mobile.reservation.models.BookingSlot
import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.station.StationFormatter
import com.solargrid.mobile.station.StationManager
import com.solargrid.mobile.station.models.Station
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

class BookSlotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookSlotBinding
    private lateinit var stationId: String

    private val reservationManager = ReservationManager.getInstance()

    // Day chips and the picked day / slot
    private val days = ReservationTime.bookableDays()
    private var selectedDay = 0
    private var slots: List<BookingSlot> = emptyList()
    private var selectedSlot: BookingSlot? = null

    // Energy request in whole kWh, kept within the picked slot's free kWh
    private var requestedKWh = DEFAULT_KWH
    private var minKWh = 1
    private var ratePerKwh = 0.0

    // Only the latest day's slot request counts
    private var slotsJob: Job? = null

    // True while the booking request is running
    private var booking = false

    private val dayAdapter = BookingDayAdapter { position -> selectDay(position) }
    private val slotAdapter = BookingSlotAdapter { slot -> selectSlot(slot) }

    // Read the station, restore the picked day/slot after rotation, then load
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookSlotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stationId = intent.getStringExtra(EXTRA_STATION_ID) ?: run {
            finish()
            return
        }
        selectedDay = savedInstanceState?.getInt(STATE_DAY)?.coerceIn(days.indices) ?: 0
        requestedKWh = savedInstanceState?.getInt(STATE_KWH, DEFAULT_KWH) ?: DEFAULT_KWH
        val restoredSlot = savedInstanceState?.getString(STATE_SLOT)

        applyWindowInsets()
        setupViews()
        loadStation()
        loadSlots(keepSlotTime = restoredSlot)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_DAY, selectedDay)
        outState.putString(STATE_SLOT, selectedSlot?.slotDateTime)
        outState.putInt(STATE_KWH, requestedKWh)
    }

    // Keep the top bar below the status bar and the Confirm button above the gesture bar
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rvDays.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDays.adapter = dayAdapter
        dayAdapter.submit(days, selectedDay)

        binding.rvSlots.layoutManager = GridLayoutManager(this, SLOT_COLUMNS)
        binding.rvSlots.adapter = slotAdapter

        binding.btnKWhMinus.setOnClickListener { setKWh(requestedKWh - 1) }
        binding.btnKWhPlus.setOnClickListener { setKWh(requestedKWh + 1) }
        binding.sliderKWh.addOnChangeListener { _, value, fromUser ->
            if (fromUser) setKWh(value.toInt())
        }

        binding.btnRetrySlots.setOnClickListener { loadSlots() }
        binding.btnConfirm.setOnClickListener { confirmBooking() }
        updateSummary()
    }

    // Name, hours and price from the map's cache first, then fresh from the API
    private fun loadStation() {
        val stationManager = StationManager.getInstance()
        stationManager.getCachedStation(stationId)?.let { bindStation(it) }
        lifecycleScope.launch {
            stationManager.getStation(stationId).onSuccess { bindStation(it) }
        }
    }

    // "06:00 – 20:00 · LKR 45 per kWh"
    private fun bindStation(station: Station) {
        binding.tvStationName.text = StationFormatter.name(this, station)
        val rate = StationFormatter.rate(this, station)
        val hours = StationFormatter.hours(this, station)
        binding.tvStationFacts.text = if (hours != null) {
            getString(R.string.booking_station_facts, hours, rate)
        } else {
            getString(R.string.booking_station_rate, rate)
        }
    }

    // New day clears the picked time and loads that day's slots
    private fun selectDay(position: Int) {
        if (booking || position == selectedDay) return
        selectedDay = position
        dayAdapter.setSelected(position)
        selectedSlot = null
        updateEnergy()
        loadSlots()
    }

    private fun selectSlot(slot: BookingSlot) {
        if (booking || !slot.isAvailable()) return
        selectedSlot = slot
        slotAdapter.setSelected(slot.slotDateTime)
        updateEnergy()
    }

    // Ask the API for the picked day; keepSlotTime re-picks a slot if it is still free
    private fun loadSlots(keepSlotTime: String? = null) {
        slotsJob?.cancel()
        showSlotsLoading()
        val date = ReservationTime.apiDate(days[selectedDay])

        slotsJob = lifecycleScope.launch {
            reservationManager.getAvailability(stationId, date)
                .onSuccess { availability ->
                    slots = availability.slots.orEmpty()
                    ratePerKwh = availability.ratePerKwh ?: 0.0
                    minKWh = ceil(availability.minKWh ?: 1.0).toInt().coerceAtLeast(1)
                    selectedSlot = slots.firstOrNull { it.slotDateTime == keepSlotTime && it.isAvailable() }
                    showSlots()
                }
                .onFailure { error ->
                    slots = emptyList()
                    selectedSlot = null
                    showSlotsMessage(error.message, showRetry = true)
                }
            updateEnergy()
        }
    }

    // Whole kWh the picked slot allows, or null when nothing is picked
    private fun kWhRange(): IntRange? {
        val slot = selectedSlot ?: return null
        val max = floor(slot.kWhLeft ?: 0.0).toInt()
        return if (max >= minKWh) minKWh..max else null
    }

    // Show the energy picker for the picked slot and keep the request inside its limit
    private fun updateEnergy() {
        val range = kWhRange()
        binding.layoutEnergy.isVisible = range != null
        if (range != null) {
            requestedKWh = requestedKWh.coerceIn(range)
            binding.sliderKWh.isVisible = range.last > range.first
            if (range.last > range.first) {
                binding.sliderKWh.valueFrom = range.first.toFloat()
                binding.sliderKWh.valueTo = range.last.toFloat()
                binding.sliderKWh.value = requestedKWh.toFloat()
            }
            binding.tvKWhHint.text = getString(
                R.string.booking_kwh_range,
                range.first.toString(), range.last.toString(), selectedSlot?.startTime.orEmpty()
            )
        }
        bindEnergy()
    }

    // −/+ and the slider change the request one kWh at a time
    private fun setKWh(value: Int) {
        val range = kWhRange() ?: return
        if (booking) return
        requestedKWh = value.coerceIn(range)
        if (binding.sliderKWh.isVisible && binding.sliderKWh.value.toInt() != requestedKWh) {
            binding.sliderKWh.value = requestedKWh.toFloat()
        }
        bindEnergy()
    }

    // "10 kWh", "Estimated LKR 450" and the −/+ limits
    private fun bindEnergy() {
        val range = kWhRange()
        binding.tvKWh.text = getString(R.string.booking_kwh_value, requestedKWh.toString())
        binding.tvEstimatedCost.text = getString(
            R.string.booking_estimated_cost, ReservationFormatter.number(requestedKWh * ratePerKwh)
        )
        binding.btnKWhMinus.isEnabled = range != null && requestedKWh > range.first
        binding.btnKWhPlus.isEnabled = range != null && requestedKWh < range.last
        updateSummary()
    }

    private fun showSlotsLoading() {
        binding.rvSlots.isVisible = false
        binding.layoutSlotsMessage.isVisible = false
        binding.progressSlots.isVisible = true
    }

    // Grid of slots, or a note when the station has none that day
    private fun showSlots() {
        binding.progressSlots.isVisible = false
        if (slots.isEmpty()) {
            showSlotsMessage(getString(R.string.booking_no_slots), showRetry = false)
            return
        }
        binding.layoutSlotsMessage.isVisible = false
        binding.rvSlots.isVisible = true
        slotAdapter.submit(slots, selectedSlot?.slotDateTime)
    }

    private fun showSlotsMessage(message: String?, showRetry: Boolean) {
        binding.progressSlots.isVisible = false
        binding.rvSlots.isVisible = false
        binding.layoutSlotsMessage.isVisible = true
        binding.tvSlotsMessage.text = message ?: getString(R.string.booking_error_slots)
        binding.btnRetrySlots.isVisible = showRetry
    }

    // Bottom bar: "Tue, 29 Sep · 09:00 – 10:00 · 10 kWh" once a slot is picked
    private fun updateSummary() {
        val slot = selectedSlot
        val canBook = slot != null && kWhRange() != null
        binding.tvSummary.text = if (slot == null || !canBook) {
            getString(R.string.booking_pick_slot)
        } else {
            getString(
                R.string.booking_summary,
                ReservationTime.dayLabel(days[selectedDay]), slot.startTime, slot.endTime, requestedKWh.toString()
            )
        }
        binding.btnConfirm.isEnabled = canBook && !booking
    }

    // Send the booking; on a rule error (e.g. slot just filled) show it and refresh the slots
    private fun confirmBooking() {
        val slot = selectedSlot ?: return
        val slotTime = slot.slotDateTime ?: return
        val kWh = requestedKWh
        setBooking(true)

        lifecycleScope.launch {
            val result = reservationManager.createReservation(stationId, slotTime, kWh.toDouble())
            setBooking(false)
            result
                .onSuccess { booked -> showBooked(slot, booked) }
                .onFailure { error ->
                    showError(error.message)
                    loadSlots()
                }
        }
    }

    private fun setBooking(inProgress: Boolean) {
        booking = inProgress
        binding.btnConfirm.setText(if (inProgress) R.string.booking_confirming else R.string.booking_confirm)
        updateSummary()
    }

    // Tell the user it's waiting for approval with the API's kWh and cost, then go to My Bookings
    private fun showBooked(slot: BookingSlot, booked: ReservationResult) {
        val kWh = booked.requestedKWh ?: requestedKWh.toDouble()
        val cost = booked.estimatedCost ?: (kWh * ratePerKwh)
        val message = getString(
            R.string.booking_success_message,
            binding.tvStationName.text,
            ReservationTime.dayLabel(days[selectedDay]),
            slot.startTime,
            ReservationFormatter.number(kWh),
            ReservationFormatter.number(cost)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.booking_success_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.booking_success_ok) { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .show()
    }

    // API rule message above the bottom bar
    private fun showError(message: String?, @StringRes fallback: Int = R.string.booking_error_book) {
        Snackbar.make(binding.root, message ?: getString(fallback), Snackbar.LENGTH_LONG)
            .setAnchorView(binding.layoutBottom)
            .show()
    }

    companion object {
        private const val EXTRA_STATION_ID = "station_id"
        private const val STATE_DAY = "selected_day"
        private const val STATE_SLOT = "selected_slot"
        private const val STATE_KWH = "requested_kwh"
        private const val SLOT_COLUMNS = 3

        // Starting energy request before the user changes it
        private const val DEFAULT_KWH = 10

        // Intent to book a slot at one station
        fun newIntent(context: Context, stationId: String): Intent =
            Intent(context, BookSlotActivity::class.java).putExtra(EXTRA_STATION_ID, stationId)
    }
}
