/*
 * File: SolarGridApp.kt
 * Description: Root navigation (login, register, main shell)
 */
package com.solargrid.prosumer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
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
import com.solargrid.prosumer.ui.login.LoginScreen
import com.solargrid.prosumer.ui.login.LoginViewModel
import com.solargrid.prosumer.ui.main.MainShell
import com.solargrid.prosumer.ui.register.RegisterScreen
import com.solargrid.prosumer.ui.register.RegisterViewModel

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
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
        mutableStateOf(if (sessionStore.isLoggedIn) Routes.MAIN else Routes.LOGIN)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
            composable(Routes.LOGIN) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.factory(authRepository),
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoggedIn = {
                        startDestination = Routes.MAIN
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onCreateAccount = {
                        navController.navigate(Routes.REGISTER)
                    },
                )
            }
            composable(Routes.REGISTER) {
                val registerViewModel: RegisterViewModel = viewModel(
                    factory = RegisterViewModel.factory(authRepository),
                )
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegistered = {
                        Toast.makeText(
                            context,
                            "Registered. Wait for operator approval, then sign in.",
                            Toast.LENGTH_LONG,
                        ).show()
                        navController.popBackStack(Routes.LOGIN, inclusive = false)
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.MAIN) {
                MainShell(
                    fullName = sessionStore.fullName,
                    userType = sessionStore.userType,
                    onSignOut = {
                        authRepository.logout()
                        startDestination = Routes.LOGIN
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                )
            }
        }
}
