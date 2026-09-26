/*
 * File: BookScreen.kt
 * Description: Book energy slot tab placeholder
 */
package com.solargrid.prosumer.ui.book

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.main.TabPlaceholder

@Composable
fun BookScreen() {
    TabPlaceholder(
        title = stringResource(R.string.book_title),
        subtitle = stringResource(R.string.book_placeholder),
        icon = Icons.Outlined.CalendarMonth,
    )
}
