/*
 * File: BookingsScreen.kt
 * Description: My bookings list tab placeholder
 */
package com.solargrid.prosumer.ui.bookings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.main.TabPlaceholder

@Composable
fun BookingsScreen() {
    TabPlaceholder(
        title = stringResource(R.string.bookings_title),
        subtitle = stringResource(R.string.bookings_placeholder),
        icon = Icons.Outlined.ReceiptLong,
    )
}
