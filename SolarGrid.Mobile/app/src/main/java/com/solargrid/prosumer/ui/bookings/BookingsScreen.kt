/*
 * File: BookingsScreen.kt
 * Description: My bookings carousel with station images
 */
package com.solargrid.prosumer.ui.bookings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText

@Composable
fun BookingsScreen() {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(SampleBookings.first().id) }
    val selected = SampleBookings.first { it.id == selectedId }

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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(SampleBookings, key = { it.id }) { booking ->
                MyBookingCard(
                    booking = booking,
                    selected = booking.id == selectedId,
                    onClick = { selectedId = booking.id },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(
                    context,
                    context.getString(R.string.bookings_refresh_toast),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
        ) {
            Text(
                text = stringResource(R.string.bookings_show_all),
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
            )
        }

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
                text = selected.slotLabel,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${selected.status} · ${selected.energyLabel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val msg = if (selected.status.equals("Approved", ignoreCase = true)) {
                        context.getString(R.string.bookings_qr_toast)
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

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.bookings_api_hint),
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
