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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solargrid.prosumer.data.BookingDraftStore
import com.solargrid.prosumer.data.DashboardRepository
import com.solargrid.prosumer.data.ReservationRepository
import com.solargrid.prosumer.data.SessionStore
import com.solargrid.prosumer.data.StationRepository
import com.solargrid.prosumer.data.api.RetrofitClient
import com.solargrid.prosumer.ui.book.BookScreen
import com.solargrid.prosumer.ui.book.BookViewModel
import com.solargrid.prosumer.ui.bookings.BookingsScreen
import com.solargrid.prosumer.ui.home.HomeScreen
import com.solargrid.prosumer.ui.home.HomeViewModel
import com.solargrid.prosumer.ui.map.MapScreen
import com.solargrid.prosumer.ui.map.MapViewModel
import com.solargrid.prosumer.ui.profile.ProfileScreen

@Composable
fun MainShell(
    sessionStore: SessionStore,
    onSignOut: () -> Unit,
) {
    val bookingDraftStore = remember { BookingDraftStore() }
    val dashboardRepository = remember {
        DashboardRepository(
            api = RetrofitClient.createDashboardApi(sessionStore),
            sessionStore = sessionStore,
        )
    }
    val stationRepository = remember {
        StationRepository(
            api = RetrofitClient.createStationsApi(sessionStore),
        )
    }
    val reservationRepository = remember {
        ReservationRepository(
            api = RetrofitClient.createReservationsApi(sessionStore),
            sessionStore = sessionStore,
        )
    }

    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun openTab(route: String) {
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { openTab(dest.route) },
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
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(
                        dashboardRepository = dashboardRepository,
                        fullName = sessionStore.fullName,
                    ),
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenMap = { openTab(MainDestination.MAP.route) },
                    onOpenBook = { openTab(MainDestination.BOOK.route) },
                    onOpenBookings = { openTab(MainDestination.BOOKINGS.route) },
                    onOpenProfile = { openTab(MainDestination.PROFILE.route) },
                )
            }
            composable(MainDestination.MAP.route) {
                val mapViewModel: MapViewModel = viewModel(
                    factory = MapViewModel.factory(stationRepository),
                )
                MapScreen(
                    viewModel = mapViewModel,
                    onBookStation = { id, name ->
                        bookingDraftStore.setStation(id, name)
                        openTab(MainDestination.BOOK.route)
                    },
                )
            }
            composable(MainDestination.BOOK.route) {
                val bookViewModel: BookViewModel = viewModel(
                    factory = BookViewModel.factory(
                        stationRepository = stationRepository,
                        reservationRepository = reservationRepository,
                        bookingDraftStore = bookingDraftStore,
                    ),
                )
                BookScreen(
                    viewModel = bookViewModel,
                    onBooked = { openTab(MainDestination.BOOKINGS.route) },
                )
            }
            composable(MainDestination.BOOKINGS.route) {
                BookingsScreen()
            }
            composable(MainDestination.PROFILE.route) {
                ProfileScreen(
                    fullName = sessionStore.fullName,
                    userType = sessionStore.userType,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}
