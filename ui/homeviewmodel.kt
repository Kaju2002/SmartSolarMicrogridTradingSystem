/*
 * File: HomeViewModel.kt
 * Description: Home dashboard state and summary load
 */
package com.solargrid.prosumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solargrid.prosumer.data.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "Hello",
    val fullName: String = "Prosumer",
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val completedCount: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val dashboardRepository: DashboardRepository,
    private val fullName: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greeting = greetingForNow(),
            fullName = fullName?.takeIf { it.isNotBlank() } ?: "Prosumer",
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    greeting = greetingForNow(),
                )
            }
            dashboardRepository.getSummary().fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            pendingCount = summary.pendingCount,
                            approvedCount = summary.approvedCount,
                            completedCount = summary.completedCount,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "Could not load summary",
                        )
                    }
                },
            )
        }
    }

    private fun greetingForNow(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Hello"
        }
    }

    companion object {
        fun factory(
            dashboardRepository: DashboardRepository,
            fullName: String?,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(dashboardRepository, fullName) as T
                }
            }
    }
}
