/*
 * File: MapScreen.kt
 * Description: Nearby stations map tab placeholder
 */
package com.solargrid.prosumer.ui.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.main.TabPlaceholder

@Composable
fun MapScreen() {
    TabPlaceholder(
        title = stringResource(R.string.map_title),
        subtitle = stringResource(R.string.map_placeholder),
        icon = Icons.Outlined.Map,
    )
}
