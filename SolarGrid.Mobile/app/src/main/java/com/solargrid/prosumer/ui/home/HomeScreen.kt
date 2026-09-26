/*
 * File: HomeScreen.kt
 * Description: Prosumer home dashboard overview
 */
package com.solargrid.prosumer.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenMap: () -> Unit,
    onOpenBook: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.greeting,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                )
                Text(
                    text = state.fullName,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                )
            }
            IconButton(
                onClick = viewModel::refresh,
                enabled = !state.loading,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.home_refresh),
                        tint = PrimaryBlue,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.home_panel_section),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF111111),
        )
        Spacer(Modifier.height(10.dp))

        BookingSummaryCard(
            pending = state.pendingCount,
            approved = state.approvedCount,
            completed = state.completedCount,
            onViewBookings = onOpenBookings,
        )

        if (!state.error.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = state.error.orEmpty(),
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.home_quick_actions),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF111111),
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickAction(
                icon = Icons.Outlined.Map,
                label = stringResource(R.string.nav_map),
                onClick = onOpenMap,
            )
            QuickAction(
                icon = Icons.Outlined.CalendarMonth,
                label = stringResource(R.string.nav_book),
                onClick = onOpenBook,
            )
            QuickAction(
                icon = Icons.Outlined.ReceiptLong,
                label = stringResource(R.string.nav_bookings),
                onClick = onOpenBookings,
            )
            QuickAction(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.nav_profile),
                onClick = onOpenProfile,
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.home_explore_section),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF111111),
        )
        Spacer(Modifier.height(10.dp))

        ExploreImageCard(
            title = stringResource(R.string.home_explore_stations_title),
            description = stringResource(R.string.home_explore_stations_body),
            imageRes = R.drawable.station_house,
            buttonText = stringResource(R.string.home_detail),
            onClick = onOpenMap,
        )
        Spacer(Modifier.height(12.dp))
        ExploreImageCard(
            title = stringResource(R.string.home_explore_book_title),
            description = stringResource(R.string.home_explore_book_body),
            imageRes = R.drawable.station_isometric,
            buttonText = stringResource(R.string.home_detail),
            onClick = onOpenBook,
        )

        Spacer(Modifier.height(16.dp))
        TipCard()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BookingSummaryCard(
    pending: Int,
    approved: Int,
    completed: Int,
    onViewBookings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_summary_title),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF111111),
        )
        Text(
            text = stringResource(R.string.home_summary_subtitle),
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.station_isometric),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(130.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                SummaryMetric(
                    value = pending.toString(),
                    label = stringResource(R.string.home_pending),
                )
                Spacer(Modifier.height(10.dp))
                SummaryMetric(
                    value = approved.toString(),
                    label = stringResource(R.string.home_approved),
                )
                Spacer(Modifier.height(10.dp))
                SummaryMetric(
                    value = completed.toString(),
                    label = stringResource(R.string.home_completed),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onViewBookings,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = SignInButtonText,
            ),
        ) {
            Text(
                text = stringResource(R.string.home_detail),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
) {
    Column {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF111111),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
        )
    }
}

@Composable
private fun ExploreImageCard(
    title: String,
    description: String,
    imageRes: Int,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF111111),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = SignInButtonText,
            ),
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryBlue,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TipCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_tip_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color(0xFF111111),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_tip_body),
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp,
        )
    }
}
