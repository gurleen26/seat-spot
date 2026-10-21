package com.example.seatspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AvailableColor = Color(0xFFC8F2D5)
private val OccupiedColor = Color(0xFFF7C7C7)
private val SelectedColor = Color(0xFFB9C9FF)

@Composable
fun SeatMapScreen(
    onBackClick: () -> Unit,
    onReserveClick: (Int) -> Unit
) {
    // Temporary demo data: false = available, true = occupied.
    val occupiedSeats = setOf(3, 6, 9, 12, 15, 18)
    var selectedSeat by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(20.dp)
    ) {
        Text(
            text = "Choose Your Seat",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2447A8)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Central Library • Ground Floor")

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(AvailableColor, "Available")
            LegendItem(OccupiedColor, "Occupied")
            LegendItem(SelectedColor, "Selected")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STUDY DESKS",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                for (rowStart in 1..16 step 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (seatNumber in rowStart..(rowStart + 3)) {
                            val isOccupied = seatNumber in occupiedSeats
                            val isSelected = selectedSeat == seatNumber

                            val seatColor = when {
                                isOccupied -> OccupiedColor
                                isSelected -> SelectedColor
                                else -> AvailableColor
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clickable(
                                        enabled = !isOccupied
                                    ) {
                                        selectedSeat = seatNumber
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = seatColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "S$seatNumber",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = if (isOccupied) "Taken" else "Seat",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = selectedSeat?.let { "Selected seat: S$it" }
                ?: "Tap an available seat to select it.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedSeat?.let(onReserveClick) },
            enabled = selectedSeat != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to Reserve")
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(label, fontSize = 11.sp)
    }
}
