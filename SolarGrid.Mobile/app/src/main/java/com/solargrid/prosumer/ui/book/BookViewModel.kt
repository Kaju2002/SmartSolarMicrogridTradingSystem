/*
 * File: BookViewModel.kt
 * Description: Book flow — stations, slot, create reservation
 */
package com.solargrid.prosumer.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solargrid.prosumer.R
import com.solargrid.prosumer.data.BookingDraftStore
import com.solargrid.prosumer.data.ReservationRepository
import com.solargrid.prosumer.data.StationRepository
import com.solargrid.prosumer.data.api.NearbyStation
import com.solargrid.prosumer.ui.map.DEMO_LAT
import com.solargrid.prosumer.ui.map.DEMO_LNG
import com.solargrid.prosumer.ui.stations.StationCardUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class DayOption(
    val label: String,
    val year: Int,
    val month: Int,
    val day: Int,
)

data class BookUiState(
    val stations: List<StationCardUi> = emptyList(),
    val selectedStationId: String = "",
    val dayOptions: List<DayOption> = emptyList(),
    val selectedDayIndex: Int = 0,
    val hourOptions: List<Int> = listOf(6, 8, 10, 12, 14, 16),
    val selectedHour: Int = 10,
    /** UI-only; API does not accept kWh yet. */
    val energyMinKwh: Int = 5,
    val energyMaxKwh: Int = 50,
    val energyPresetsKwh: List<Int> = listOf(5, 10, 15, 20, 30, 50),
    val selectedEnergyKwh: Int = 10,
    val loadingStations: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

class BookViewModel(
    private val stationRepository: StationRepository,
    private val reservationRepository: ReservationRepository,
    private val bookingDraftStore: BookingDraftStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BookUiState(dayOptions = buildNextSevenDays()),
    )
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    init {
        loadStations()
    }

    /** Re-apply Map → Book draft when the Book tab is shown again. */
    fun onScreenVisible() {
        val draftId = bookingDraftStore.stationId
        if (!draftId.isNullOrBlank() &&
            _uiState.value.stations.any { it.id == draftId } &&
            _uiState.value.selectedStationId != draftId
        ) {
            _uiState.update { it.copy(selectedStationId = draftId, error = null) }
        }
    }

    fun loadStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingStations = true, error = null, successMessage = null) }
            stationRepository.getNearby(DEMO_LAT, DEMO_LNG, radiusKm = 15.0).fold(
                onSuccess = { list ->
                    val cards = list.map { it.toCardUi() }
                    val draftId = bookingDraftStore.stationId
                    val selected = when {
                        !draftId.isNullOrBlank() && cards.any { it.id == draftId } -> draftId
                        cards.any { it.id == _uiState.value.selectedStationId } ->
                            _uiState.value.selectedStationId
                        cards.isNotEmpty() -> cards.first().id
                        else -> ""
                    }
                    _uiState.update {
                        it.copy(
                            loadingStations = false,
                            stations = cards,
                            selectedStationId = selected,
                            error = if (cards.isEmpty()) {
                                "No nearby stations. Open Map or add stations in Backoffice."
                            } else {
                                null
                            },
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loadingStations = false,
                            error = err.message ?: "Could not load stations",
                        )
                    }
                },
            )
        }
    }

    fun selectStation(id: String) {
        _uiState.update { it.copy(selectedStationId = id, error = null, successMessage = null) }
        val name = _uiState.value.stations.firstOrNull { it.id == id }?.name.orEmpty()
        bookingDraftStore.setStation(id, name)
    }

    fun selectDay(index: Int) {
        _uiState.update { it.copy(selectedDayIndex = index, error = null, successMessage = null) }
    }

    fun selectHour(hour: Int) {
        _uiState.update { it.copy(selectedHour = hour, error = null, successMessage = null) }
    }

    fun selectEnergy(kwh: Int) {
        val state = _uiState.value
        val clamped = kwh.coerceIn(state.energyMinKwh, state.energyMaxKwh)
        _uiState.update {
            it.copy(selectedEnergyKwh = clamped, error = null, successMessage = null)
        }
    }

    fun submit() {
        val state = _uiState.value
        val stationId = state.selectedStationId
        if (stationId.isBlank()) {
            _uiState.update { it.copy(error = "Select a station.") }
            return
        }
        val day = state.dayOptions.getOrNull(state.selectedDayIndex)
        if (day == null) {
            _uiState.update { it.copy(error = "Select a date.") }
            return
        }

        val localSlot = Calendar.getInstance().apply {
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.month)
            set(Calendar.DAY_OF_MONTH, day.day)
            set(Calendar.HOUR_OF_DAY, state.selectedHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (localSlot.timeInMillis <= System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Pick a future date and time.") }
            return
        }

        val iso = formatUtcIso(localSlot)
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null, successMessage = null) }
            reservationRepository.create(stationId, iso).fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            successMessage = body.message.ifBlank {
                                "Reservation created (${body.status ?: "Pending"})"
                            },
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            error = err.message ?: "Booking failed",
                        )
                    }
                },
            )
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun buildNextSevenDays(): List<DayOption> {
        val labelFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        return (0..6).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            DayOption(
                label = if (offset == 0) "Today" else labelFmt.format(c.time),
                year = c.get(Calendar.YEAR),
                month = c.get(Calendar.MONTH),
                day = c.get(Calendar.DAY_OF_MONTH),
            )
        }
    }

    private fun formatUtcIso(local: Calendar): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(local.time)
    }

    private fun NearbyStation.toCardUi(): StationCardUi {
        val dist = distanceKm
        val rate = ratePerKwh?.takeIf { it > 0 }
        return StationCardUi(
            id = stationId.orEmpty(),
            name = stationName ?: "Station",
            location = if (dist != null) {
                String.format(Locale.US, "%.1f km away", dist)
            } else {
                "Nearby"
            },
            capacityKw = "Active",
            priceLabel = if (rate != null) {
                String.format(Locale.US, "LKR %.0f / kWh", rate)
            } else {
                "Rate pending"
            },
            imageRes = R.drawable.station_house,
            ratePerKwh = rate,
        )
    }

    companion object {
        fun factory(
            stationRepository: StationRepository,
            reservationRepository: ReservationRepository,
            bookingDraftStore: BookingDraftStore,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookViewModel(
                        stationRepository,
                        reservationRepository,
                        bookingDraftStore,
                    ) as T
                }
            }
    }
}
