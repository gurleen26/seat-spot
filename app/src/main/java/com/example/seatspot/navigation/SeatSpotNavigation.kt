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
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seatspot.ui.screens.MyReservationsScreen
import com.example.seatspot.viewmodel.MyReservationsViewModel
import com.example.seatspot.ui.screens.ProfileScreen
import com.example.seatspot.ui.screens.DiscussionRoomsScreen
import androidx.compose.runtime.*
import com.example.seatspot.data.model.DiscussionRoom
import com.example.seatspot.data.repository.DiscussionRoomRepository
import com.example.seatspot.ui.screens.DiscussionRoomBookingScreen

@Composable
fun SeatSpotNavigation() {
    val navController = rememberNavController()

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        composable("splash") {

            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {

                LaunchedEffect(Unit) {
                    navController.navigate("home") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

            } else {

                SplashScreen(
                    onContinue = {
                        navController.navigate("register")
                    },
                    onSignIn = {
                        navController.navigate("login")
                    }
                )
            }
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
                    navController.navigate("home") {
                        popUpTo("register") {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate("login") {
                        popUpTo("register") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onExploreSeatsClick = {
                    navigateToTab("seatmap")
                },
                onMyReservationsClick = {
                    navigateToTab("my_reservations")
                },
                onProfileClick = {
                    navigateToTab("profile")
                },
                onDiscussionRoomsClick = {
                    navController.navigate("discussion_rooms")
                }
            )
        }

        composable("discussion_rooms") {
            DiscussionRoomsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRoomClick = { room ->
                    navController.navigate(
                        "discussion_room_booking/${room.roomId}"
                    )
                }
            )
        }

        composable("discussion_room_booking/{roomId}") { backStackEntry ->

            val roomId = backStackEntry.arguments?.getString("roomId")

            var selectedRoom by remember {
                mutableStateOf<DiscussionRoom?>(null)
            }

            LaunchedEffect(roomId) {
                DiscussionRoomRepository().getDiscussionRooms { result ->
                    selectedRoom = result.getOrNull()
                        ?.find { it.roomId == roomId }
                }
            }

            selectedRoom?.let { room ->
                DiscussionRoomBookingScreen(
                    room = room,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onBookClick = { groupSize ->
                        // Booking API will be connected next
                    }
                )
            }
        }

        composable("profile") {
            ProfileScreen(
                onMyReservationsClick = {
                    navigateToTab("my_reservations")
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onTabClick = { route ->
                    navigateToTab(route)
                }
            )
        }

        composable("my_reservations") {

            var idToken by remember {
                mutableStateOf<String?>(null)
            }

            var tokenError by remember {
                mutableStateOf<String?>(null)
            }

            val currentUser = FirebaseAuth.getInstance().currentUser

            LaunchedEffect(currentUser?.uid) {

                if (currentUser == null) {
                    tokenError = "Please sign in to view reservations."
                } else {
                    currentUser.getIdToken(false)
                        .addOnSuccessListener { result ->
                            idToken = result.token
                        }
                        .addOnFailureListener { exception ->
                            tokenError = exception.localizedMessage
                                ?: "Unable to authenticate user."
                        }
                }
            }

            val reservationsViewModel: MyReservationsViewModel = viewModel()

            when {
                idToken != null -> {
                    MyReservationsScreen(
                        idToken = idToken!!,
                        onBackClick = { navController.popBackStack() },
                        viewModel = reservationsViewModel,
                        onTabClick = { route ->
                            navigateToTab(route)
                        }
                    )
                }

                tokenError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tokenError!!)
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }


        composable("seatmap") {
            SeatMapScreen(
                onBackClick = { navController.popBackStack() },
                onReserveClick = { resourceId, resourceNumber, areaId, sectionId, tableId,
                                   reservationDate, startTime, endTime, resourceType ->

                    navController.navigate(
                        "reservation/${android.net.Uri.encode(resourceId)}/$resourceNumber/" +
                                "${android.net.Uri.encode(areaId)}/" +
                                "${android.net.Uri.encode(sectionId)}/" +
                                "${android.net.Uri.encode(tableId)}/" +
                                "$reservationDate/$startTime/$endTime/$resourceType"
                    )
                },
                onTabClick = { route ->
                    navigateToTab(route)
                }
            )
        }

        composable(
            route = "reservation/{seatId}/{seatNumber}/{areaId}/{sectionId}/{tableId}/{reservationDate}/{startTime}/{endTime}/{resourceType}",
            arguments = listOf(
                navArgument("seatId") {
                    type = NavType.StringType
                },
                navArgument("seatNumber") {
                    type = NavType.IntType
                },
                navArgument("areaId") {
                    type = NavType.StringType
                },
                navArgument("sectionId") {
                    type = NavType.StringType
                },
                navArgument("tableId") {
                    type = NavType.StringType
                },
                navArgument("reservationDate") {
                    type = NavType.StringType
                },
                navArgument("startTime") {
                    type = NavType.StringType
                },
                navArgument("endTime") {
                    type = NavType.StringType
                },
                navArgument("resourceType") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val seatId = backStackEntry.arguments?.getString("seatId") ?: ""
            val seatNumber = backStackEntry.arguments?.getInt("seatNumber") ?: 1
            val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
            val sectionId = backStackEntry.arguments?.getString("sectionId") ?: ""
            val tableId = backStackEntry.arguments?.getString("tableId") ?: ""
            val reservationDate =
                backStackEntry.arguments?.getString("reservationDate") ?: ""

            val startTime =
                backStackEntry.arguments?.getString("startTime") ?: ""

            val endTime =
                backStackEntry.arguments?.getString("endTime") ?: ""

            val resourceType =
                backStackEntry.arguments?.getString("resourceType") ?: "SEAT"

            ReservationScreen(
                seatId = seatId,
                seatNumber = seatNumber,
                areaId = areaId,
                sectionId = sectionId,
                tableId = tableId,
                onBackClick = {
                    navController.popBackStack()
                },
                reservationDate = reservationDate,
                startTime = startTime,
                endTime = endTime,
                resourceType = resourceType,
                labId = if (resourceType == "COMPUTER") areaId else ""
            )
        }
    }
}
