package com.example.seatspot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeepIndigo = Color(0xFF1B2A6B)
private val RoyalBlue = Color(0xFF2447A8)
private val Teal = Color(0xFF1FB5A8)
private val SecondaryText = Color(0xFF64748B)

private data class BottomNavDestination(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun SeatSpotBottomBar(
    selectedRoute: String,
    onDestinationClick: (String) -> Unit
) {
    val destinations = listOf(
        BottomNavDestination(
            label = "Home",
            icon = Icons.Default.Home,
            route = "home"
        ),
        BottomNavDestination(
            label = "Seats",
            icon = Icons.Default.EventSeat,
            route = "seatmap"
        ),
        BottomNavDestination(
            label = "Bookings",
            icon = Icons.Default.CalendarMonth,
            route = "my_reservations"
        ),
        BottomNavDestination(
            label = "Profile",
            icon = Icons.Default.Person,
            route = "profile"
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                val selected = selectedRoute == destination.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!selected) {
                                onDestinationClick(destination.route)
                            }
                        }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) RoyalBlue else SecondaryText,
                        modifier = Modifier.size(23.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = destination.label,
                        color = if (selected) RoyalBlue else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Spacer(
                        modifier = Modifier
                            .size(
                                width = 18.dp,
                                height = 3.dp
                            )
                            .background(
                                color = if (selected) Teal else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
