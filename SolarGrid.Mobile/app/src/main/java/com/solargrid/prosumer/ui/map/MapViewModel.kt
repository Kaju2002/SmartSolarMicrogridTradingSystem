/*
 * File: MapViewModel.kt
 * Description: Map location and nearby stations state
 */
package com.solargrid.prosumer.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solargrid.prosumer.data.StationRepository
import com.solargrid.prosumer.data.api.NearbyStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Colombo fallback for emulator / missing GPS. */
const val DEMO_LAT = 6.9271
const val DEMO_LNG = 79.8612

data class MapUiState(
    val userLat: Double = DEMO_LAT,
    val userLng: Double = DEMO_LNG,
    val usingDemoLocation: Boolean = true,
    val stations: List<NearbyStation> = emptyList(),
    val selectedStationId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class MapViewModel(
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadNearby(DEMO_LAT, DEMO_LNG, usingDemo = true)
    }

    fun onLocationResolved(lat: Double, lng: Double, isDemo: Boolean) {
        loadNearby(lat, lng, usingDemo = isDemo)
    }

    fun selectStation(stationId: String?) {
        _uiState.update { it.copy(selectedStationId = stationId) }
    }

    fun refresh() {
        val s = _uiState.value
        loadNearby(s.userLat, s.userLng, usingDemo = s.usingDemoLocation)
    }

    private fun loadNearby(lat: Double, lng: Double, usingDemo: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    userLat = lat,
                    userLng = lng,
                    usingDemoLocation = usingDemo,
                )
            }
            stationRepository.getNearby(lat, lng).fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            stations = list,
                            error = if (list.isEmpty()) {
                                "No active stations within 15 km. Add stations near Colombo in Backoffice."
                            } else {
                                null
                            },
                            selectedStationId = list.firstOrNull()?.stationId,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "Failed to load stations",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(stationRepository: StationRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MapViewModel(stationRepository) as T
                }
            }
    }
}
