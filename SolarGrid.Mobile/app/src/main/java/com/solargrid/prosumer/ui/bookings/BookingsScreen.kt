/*
 * File: BookingsScreen.kt
 * Description: My bookings from API with station cards
 */
package com.solargrid.prosumer.ui.bookings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selected = state.bookings.firstOrNull { it.id == state.selectedId }

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.bookings_title),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF111111),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.bookings_choose_hint),
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        when {
            state.loading && state.bookings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            state.bookings.isEmpty() -> {
                Text(
                    text = state.error
                        ?: stringResource(R.string.bookings_empty),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.bookings, key = { it.id }) { booking ->
                        MyBookingCard(
                            booking = booking,
                            selected = booking.id == state.selectedId,
                            onClick = { viewModel.selectBooking(booking.id) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = viewModel::refresh,
            enabled = !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
        ) {
            Text(
                text = if (state.loading) {
                    stringResource(R.string.bookings_refreshing)
                } else {
                    stringResource(R.string.bookings_refresh)
                },
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!state.error.isNullOrBlank() && state.bookings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.error.orEmpty(),
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (selected != null) {
            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.bookings_selected_label),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = selected.stationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF111111),
                )
                Text(
                    text = selected.location,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected.slotLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                        modifier = Modifier.weight(1f),
                    )
                    BookingStatusPill(selected.status)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val msg = if (selected.status.equals("Approved", ignoreCase = true)) {
                            if (!selected.qrCode.isNullOrBlank()) {
                                context.getString(R.string.bookings_qr_toast)
                            } else {
                                context.getString(R.string.bookings_qr_missing)
                            }
                        } else {
                            context.getString(R.string.bookings_detail_toast, selected.status)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = SignInButtonText,
                    ),
                ) {
                    Text(
                        text = if (selected.status.equals("Approved", ignoreCase = true)) {
                            stringResource(R.string.bookings_show_qr)
                        } else {
                            stringResource(R.string.bookings_view_detail)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
