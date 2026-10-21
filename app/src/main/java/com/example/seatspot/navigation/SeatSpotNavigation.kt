package com.example.seatspot.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seatspot.ui.screens.LoginScreen
import com.example.seatspot.ui.screens.RegisterScreen
import com.example.seatspot.ui.screens.SplashScreen
import com.example.seatspot.ui.screens.HomeScreen
import com.example.seatspot.ui.screens.SeatMapScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.seatspot.ui.screens.ReservationScreen

@Composable
fun SeatSpotNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onContinue = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginClick = {
                    navController.navigate("home") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterClick = {
                    navController.navigate("login") {
                        popUpTo("register") {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreen(
                onExploreSeatsClick = {
                    navController.navigate("seatmap")
                }
            )
        }

        composable("seatmap") {
            SeatMapScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onReserveClick = { seatNumber ->
                    navController.navigate("reservation/$seatNumber")
                }
            )
        }

        composable(
            route = "reservation/{seatNumber}",
            arguments = listOf(
                navArgument("seatNumber") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val seatNumber =
                backStackEntry.arguments?.getInt("seatNumber") ?: 1

            ReservationScreen(
                seatNumber = seatNumber,
                onBackClick = {
                    navController.popBackStack()
                },
                onConfirmClick = { date, timeRange ->
                    // Real booking and confirmation will be implemented later.
                }
            )
        }
    }
}
