/*
 * File: MainActivity.kt
 * Description: App entry activity and Compose host
 */
package com.solargrid.prosumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.solargrid.prosumer.ui.SolarGridApp
import com.solargrid.prosumer.ui.theme.SolarGridMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarGridMobileTheme(dynamicColor = false) {
                SolarGridApp()
            }
        }
    }
}
