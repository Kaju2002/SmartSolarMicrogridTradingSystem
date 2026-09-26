/*
 * File: BookScreen.kt
 * Description: Book energy — station, date, time, create API
 */
package com.solargrid.prosumer.ui.book

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.stations.StationBookingCard
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText
import java.util.Locale

@Composable
fun BookScreen(
    viewModel: BookViewModel,
    onBooked: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    LaunchedEffect(state.successMessage) {
        val msg = state.successMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearSuccess()
            onBooked()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.book_title),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF111111),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.book_choose_station),
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(14.dp))

        if (state.loadingStations) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.stations, key = { it.id }) { station ->
                    StationBookingCard(
                        station = station,
                        selected = station.id == state.selectedStationId,
                        onClick = { viewModel.selectStation(station.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.book_date_label))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.dayOptions.forEachIndexed { index, day ->
                ChoiceChip(
                    label = day.label,
                    selected = index == state.selectedDayIndex,
                    onClick = { viewModel.selectDay(index) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.book_time_label))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.hourOptions.forEach { hour ->
                ChoiceChip(
                    label = String.format("%02d:00", hour),
                    selected = hour == state.selectedHour,
                    onClick = { viewModel.selectHour(hour) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.book_energy_label))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.book_energy_subtitle),
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.energyPresetsKwh.forEach { kwh ->
                ChoiceChip(
                    label = "$kwh kWh",
                    selected = kwh == state.selectedEnergyKwh,
                    onClick = { viewModel.selectEnergy(kwh) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Slider(
                value = state.selectedEnergyKwh.toFloat(),
                onValueChange = { viewModel.selectEnergy(it.toInt()) },
                valueRange = state.energyMinKwh.toFloat()..state.energyMaxKwh.toFloat(),
                steps = (state.energyMaxKwh - state.energyMinKwh) - 1,
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = Color(0xFFE5E7EB),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.book_energy_min, state.energyMinKwh),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = stringResource(R.string.book_energy_max, state.energyMaxKwh),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.book_energy_value, state.selectedEnergyKwh),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF111111),
            )
            val selectedRate = state.stations
                .firstOrNull { it.id == state.selectedStationId }
                ?.ratePerKwh
                ?.takeIf { it > 0 }
            if (selectedRate != null) {
                val estimate = state.selectedEnergyKwh * selectedRate
                Text(
                    text = stringResource(
                        R.string.book_energy_estimate,
                        String.format(Locale.getDefault(), "%,.0f", estimate),
                    ),
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                Text(
                    text = String.format(Locale.US, "LKR %.0f / kWh", selectedRate),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                )
            } else {
                Text(
                    text = stringResource(R.string.book_energy_rate_missing),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.book_energy_hint),
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
            )
        }

        if (!state.error.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.error.orEmpty(),
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        val selectedName = state.stations
            .firstOrNull { it.id == state.selectedStationId }
            ?.name
            ?: "—"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.book_selected_label),
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
            )
            Text(
                text = selectedName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111111),
            )
            val dayLabel = state.dayOptions
                .getOrNull(state.selectedDayIndex)
                ?.label
                .orEmpty()
            Text(
                text = "$dayLabel · ${String.format("%02d:00", state.selectedHour)} · ${state.selectedEnergyKwh} kWh",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting && state.selectedStationId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = SignInButtonText,
                    disabledContainerColor = Color(0xFFE8E8E8),
                    disabledContentColor = Color(0xFF9A9A9A),
                ),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(22.dp)
                            .width(22.dp),
                        strokeWidth = 2.dp,
                        color = SignInButtonText,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.book_confirm),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.book_api_hint),
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = Color(0xFF111111),
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AccentGold else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) AccentGold else Color(0xFFE5E7EB),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            color = if (selected) SignInButtonText else Color(0xFF333333),
        )
    }
}
