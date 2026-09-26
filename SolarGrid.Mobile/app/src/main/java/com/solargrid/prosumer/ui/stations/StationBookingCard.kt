/*
 * File: StationBookingCard.kt
 * Description: Station image card for booking selection
 */
package com.solargrid.prosumer.ui.stations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue

@Composable
fun StationBookingCard(
    station: StationCardUi,
    selected: Boolean,
    modifier: Modifier = Modifier.width(280.dp),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AccentGold else Color(0xFFE5E7EB),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Image(
            painter = painterResource(station.imageRes),
            contentDescription = station.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111111)),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = station.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = station.location,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.station_capacity_label),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = station.capacityKw,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PrimaryBlue,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.station_rate_label),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = station.priceLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111111),
                )
            }
        }
    }
}
