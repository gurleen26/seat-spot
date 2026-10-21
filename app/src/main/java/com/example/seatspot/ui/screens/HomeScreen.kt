package com.example.seatspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    onExploreSeatsClick: () -> Unit
) {
    val userName = FirebaseAuth.getInstance()
        .currentUser
        ?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Student"

    val totalSeats = 120
    val availableSeats = 38
    val occupiedSeats = totalSeats - availableSeats

    Column(
        modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Hello, $userName 👋",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2447A8)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Find your perfect study spot.",
            color = Color(0xFF666666),
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2447A8)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Central Library",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Check seat availability before you arrive.",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SeatStat(
                        label = "Total seats",
                        count = totalSeats,
                        color = Color.White
                    )

                    SeatStat(
                        label = "Available",
                        count = availableSeats,
                        color = Color(0xFF9EF0BD)
                    )

                    SeatStat(
                        label = "Occupied",
                        count = occupiedSeats,
                        color = Color(0xFFFFD3D3)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onExploreSeatsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Explore Seats")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quick tip",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Text(
                text = "Reserve an available seat before your study session to plan your visit.",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF555B66)
            )
        }
    }
}

@Composable
private fun SeatStat(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}
