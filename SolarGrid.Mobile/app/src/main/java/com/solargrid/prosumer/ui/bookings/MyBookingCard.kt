/*
 * File: MyBookingCard.kt
 * Description: Trip-planner style booking card with station image
 */
package com.solargrid.prosumer.ui.bookings

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
fun MyBookingCard(
    booking: BookingCardUi,
    selected: Boolean,
    modifier: Modifier = Modifier.width(300.dp),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val statusColor = when (booking.status.lowercase()) {
        "approved" -> PrimaryBlue
        "pending" -> Color(0xFFFF9800)
        "completed" -> Color(0xFF2E7D32)
        else -> Color(0xFF6B7280)
    }

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
            painter = painterResource(booking.imageRes),
            contentDescription = booking.stationName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111111)),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = booking.stationName,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF111111),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = booking.location,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = booking.status,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = statusColor,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bookings_slot_label),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = booking.slotLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.bookings_energy_label),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                )
                Text(
                    text = booking.energyLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111),
                )
            }
        }
    }
}
