package com.example.seatspot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seatspot.data.model.DiscussionRoom
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@Composable
fun DiscussionRoomBookingScreen(
    room: DiscussionRoom,
    onBackClick: () -> Unit,
    onBookClick: (Int) -> Unit
) {
    var groupSize by remember { mutableIntStateOf(2) }

    val context = LocalContext.current

    var selectedDate by remember {
        mutableStateOf("")
    }

    var duration by remember {
        mutableIntStateOf(60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = room.roomName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Room capacity: ${room.capacity} students")
        Button(
            onClick = {
                val calendar = Calendar.getInstance()

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        selectedDate = String.format(
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                        )
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = System.currentTimeMillis()
                }.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedDate.isEmpty()) "Select Booking Date"
                else selectedDate
            )
        }
        Text("Select Duration")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(60, 90, 120).forEach { minutes ->
                FilterChip(
                    selected = duration == minutes,
                    onClick = { duration = minutes },
                    label = { Text("$minutes min") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Select group size")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (2..6).forEach { size ->
                FilterChip(
                    selected = groupSize == size,
                    onClick = { groupSize = size },
                    label = { Text("$size") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onBookClick(groupSize) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        TextButton(onClick = onBackClick) {
            Text("Back")
        }
    }
}