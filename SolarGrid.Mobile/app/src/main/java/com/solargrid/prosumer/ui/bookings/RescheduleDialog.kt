/*
 * File: RescheduleDialog.kt
 * Description: Pick a new date and time for a booking
 */
package com.solargrid.prosumer.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.PrimaryBlue
import com.solargrid.prosumer.ui.theme.SignInButtonText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val RESCHEDULE_HOURS = listOf(6, 8, 10, 12, 14, 16)

@Composable
fun RescheduleDialog(
    booking: BookingCardUi,
    onConfirm: (dayOffset: Int, hour: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val dayLabels = remember {
        val fmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        (0..6).map { offset ->
            if (offset == 0) {
                "Today"
            } else {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                fmt.format(c.time)
            }
        }
    }
    val (initialDay, initialHour) = remember(booking.slotMillis) { initialSelection(booking.slotMillis) }
    var dayOffset by remember { mutableIntStateOf(initialDay) }
    var hour by remember { mutableIntStateOf(initialHour) }
    val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.reschedule_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = booking.stationName,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                )
                Text(
                    text = stringResource(R.string.reschedule_current, booking.slotLabel),
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                )
                Spacer(Modifier.height(14.dp))
                Text(text = stringResource(R.string.book_date_label), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        SlotChip(
                            label = label,
                            selected = index == dayOffset,
                            enabled = true,
                            onClick = { dayOffset = index },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(text = stringResource(R.string.book_time_label), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RESCHEDULE_HOURS.forEach { h ->
                        SlotChip(
                            label = String.format(Locale.US, "%02d:00", h),
                            selected = h == hour,
                            enabled = dayOffset > 0 || h > nowHour,
                            onClick = { hour = h },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.reschedule_rule, CHANGE_LOCK_HOURS),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dayOffset, hour) }) {
                Text(
                    text = stringResource(R.string.reschedule_save),
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.reschedule_keep), color = Color(0xFF6B7280))
            }
        },
    )
}

private fun initialSelection(slotMillis: Long): Pair<Int, Int> {
    if (slotMillis <= 0) return 1 to RESCHEDULE_HOURS[2]
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val slot = Calendar.getInstance().apply { timeInMillis = slotMillis }
    val slotDay = (slot.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val offset = ((slotDay.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
    val hour = slot.get(Calendar.HOUR_OF_DAY)
    return offset.coerceIn(0, 6) to (if (hour in RESCHEDULE_HOURS) hour else RESCHEDULE_HOURS[2])
}

@Composable
private fun SlotChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val textColor = when {
        !enabled -> Color(0xFFC4C7CC)
        selected -> SignInButtonText
        else -> Color(0xFF333333)
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected && enabled) AccentGold else Color.White)
            .border(
                width = 1.dp,
                color = if (selected && enabled) AccentGold else Color(0xFFE5E7EB),
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}
