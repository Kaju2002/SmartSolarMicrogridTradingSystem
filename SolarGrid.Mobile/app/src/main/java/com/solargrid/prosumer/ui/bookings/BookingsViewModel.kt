/*
 * File: BookingsViewModel.kt
 * Description: My Bookings list from reservations API
 */
package com.solargrid.prosumer.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solargrid.prosumer.R
import com.solargrid.prosumer.data.ReservationRepository
import com.solargrid.prosumer.data.StationRepository
import com.solargrid.prosumer.data.api.NearbyStation
import com.solargrid.prosumer.data.api.ReservationItem
import com.solargrid.prosumer.ui.map.DEMO_LAT
import com.solargrid.prosumer.ui.map.DEMO_LNG
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class BookingsUiState(
    val bookings: List<BookingCardUi> = emptyList(),
    val selectedId: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class BookingsViewModel(
    private val reservationRepository: ReservationRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onScreenVisible() {
        refresh()
    }

    fun selectBooking(id: String) {
        _uiState.update { it.copy(selectedId = id, error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val reservationsDeferred = async { reservationRepository.listMine() }
                val stationsDeferred = async {
                    stationRepository.getNearby(DEMO_LAT, DEMO_LNG, radiusKm = 100.0)
                }

                val reservationsResult = reservationsDeferred.await()
                val stationsById = stationsDeferred.await().getOrNull()
                    ?.filter { !it.stationId.isNullOrBlank() }
                    ?.associateBy { it.stationId.orEmpty() }
                    .orEmpty()

                reservationsResult.fold(
                    onSuccess = { list ->
                        val cards = list
                            .map { it.toCardUi(stationsById) }
                            .sortedByDescending { it.slotSortKey }
                        val selected = when {
                            cards.any { it.card.id == _uiState.value.selectedId } ->
                                _uiState.value.selectedId
                            cards.isNotEmpty() -> cards.first().card.id
                            else -> ""
                        }
                        _uiState.update {
                            it.copy(
                                loading = false,
                                bookings = cards.map { sorted -> sorted.card },
                                selectedId = selected,
                                error = if (cards.isEmpty()) {
                                    "No bookings yet. Create one from the Book tab."
                                } else {
                                    null
                                },
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = err.message ?: "Could not load bookings",
                            )
                        }
                    },
                )
            }
        }
    }

    private data class SortedCard(val card: BookingCardUi, val slotSortKey: Long)

    private fun ReservationItem.toCardUi(stationsById: Map<String, NearbyStation>): SortedCard {
        val sid = stationId.orEmpty()
        val station = stationsById[sid]
        val (label, sortKey) = formatSlot(reservationDateTime)
        val statusText = status?.takeIf { it.isNotBlank() } ?: "Pending"
        val distance = station?.distanceKm
        val image = if (sid.hashCode() % 2 == 0) {
            R.drawable.station_house
        } else {
            R.drawable.station_isometric
        }
        return SortedCard(
            card = BookingCardUi(
                id = id.orEmpty(),
                stationId = sid,
                stationName = station?.stationName ?: "Solar station",
                location = if (distance != null) {
                    String.format(Locale.US, "%.1f km away", distance)
                } else {
                    "Microgrid station"
                },
                status = statusText,
                slotLabel = label,
                qrCode = qrCode?.takeIf { it.isNotBlank() },
                imageRes = image,
            ),
            slotSortKey = sortKey,
        )
    }

    private fun formatSlot(raw: String?): Pair<String, Long> {
        if (raw.isNullOrBlank()) return "Time TBD" to 0L
        val parsers = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        )
        parsers.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }
        val date = parsers.firstNotNullOfOrNull { fmt ->
            runCatching { fmt.parse(raw) }.getOrNull()
        }
        if (date == null) return raw to 0L
        val out = SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault())
        return out.format(date) to date.time
    }

    companion object {
        fun factory(
            reservationRepository: ReservationRepository,
            stationRepository: StationRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingsViewModel(
                        reservationRepository,
                        stationRepository,
                    ) as T
                }
            }
    }
}
