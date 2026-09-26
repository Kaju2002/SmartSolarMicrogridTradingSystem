/*
 * File: BookScreen.kt
 * Description: Book energy — choose station cards
 */
package com.solargrid.prosumer.ui.book

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.solargrid.prosumer.ui.stations.SampleStations
import com.solargrid.prosumer.ui.stations.StationBookingCard
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.SignInButtonText

@Composable
fun BookScreen() {
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(SampleStations.first().id) }
    val selected = SampleStations.first { it.id == selectedId }

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

        Spacer(Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(SampleStations, key = { it.id }) { station ->
                StationBookingCard(
                    station = station,
                    selected = station.id == selectedId,
                    onClick = { selectedId = station.id },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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
                text = selected.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111111),
            )
            Text(
                text = "${selected.location} · ${selected.capacityKw}",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.book_coming_soon_toast, selected.name),
                        Toast.LENGTH_SHORT,
                    ).show()
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
                    text = stringResource(R.string.book_continue),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.book_hint),
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
