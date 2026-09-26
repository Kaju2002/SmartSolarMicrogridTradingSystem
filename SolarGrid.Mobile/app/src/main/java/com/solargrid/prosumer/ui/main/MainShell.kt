/*
 * File: MainShell.kt
 * Description: Logged-in shell with bottom navigation
 */
package com.solargrid.prosumer.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solargrid.prosumer.ui.book.BookScreen
import com.solargrid.prosumer.ui.bookings.BookingsScreen
import com.solargrid.prosumer.ui.home.HomeScreen
import com.solargrid.prosumer.ui.map.MapScreen
import com.solargrid.prosumer.ui.profile.ProfileScreen

@Composable
fun MainShell(
    fullName: String?,
    userType: String?,
    onSignOut: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNavController.navigate(dest.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = stringResource(dest.labelRes),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(dest.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(MainDestination.HOME.route) {
                HomeScreen(
                    fullName = fullName,
                    userType = userType,
                )
            }
            composable(MainDestination.MAP.route) {
                MapScreen()
            }
            composable(MainDestination.BOOK.route) {
                BookScreen()
            }
            composable(MainDestination.BOOKINGS.route) {
                BookingsScreen()
            }
            composable(MainDestination.PROFILE.route) {
                ProfileScreen(
                    fullName = fullName,
                    userType = userType,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}
