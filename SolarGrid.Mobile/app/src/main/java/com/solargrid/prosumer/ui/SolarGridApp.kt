package com.solargrid.prosumer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solargrid.prosumer.data.AuthRepository
import com.solargrid.prosumer.data.SessionStore
import com.solargrid.prosumer.data.api.RetrofitClient
import com.solargrid.prosumer.ui.home.HomeScreen
import com.solargrid.prosumer.ui.login.LoginScreen
import com.solargrid.prosumer.ui.login.LoginViewModel

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun SolarGridApp() {
    val context = LocalContext.current.applicationContext
    val sessionStore = remember { SessionStore(context) }
    val authRepository = remember {
        AuthRepository(
            api = RetrofitClient.createAuthApi(sessionStore),
            sessionStore = sessionStore,
        )
    }

    val navController = rememberNavController()
    var startDestination by remember {
        mutableStateOf(if (sessionStore.isLoggedIn) Routes.HOME else Routes.LOGIN)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.factory(authRepository),
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoggedIn = {
                        startDestination = Routes.HOME
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    fullName = sessionStore.fullName,
                    userType = sessionStore.userType,
                    onSignOut = {
                        authRepository.logout()
                        startDestination = Routes.LOGIN
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
