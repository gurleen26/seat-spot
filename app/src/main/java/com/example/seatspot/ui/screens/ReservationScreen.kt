
package com.example.seatspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val SeatSpotBlue = Color(0xFF2447A8)
private val SeatSpotBackground = Color(0xFFF5F7FA)
private val SeatSpotGreen = Color(0xFF18794E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    seatNumber: Int,
    onBackClick: () -> Unit,
    onConfirmClick: (String, String) -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val todayUtcMillis = remember {
        java.util.Calendar.getInstance(
            TimeZone.getTimeZone("UTC")
        ).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var choosingStartTime by remember { mutableStateOf(true) }

    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(11) }
    var endMinute by remember { mutableStateOf(0) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayUtcMillis
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeatSpotBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    )  {
        Text(
            text = "Reserve Your Seat",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = SeatSpotBlue
        )

        Text(
            text = "Choose when you want to study.",
            color = Color(0xFF606775),
            fontSize = 15.sp
        )

        // Selected seat details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SeatSpotBlue
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Central Library",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Ground Floor",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "S$seatNumber",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeatSpotBlue
                        )
                        Text("Your seat", fontSize = 11.sp)
                    }
                }
            }
        }

        // Date selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Reservation date",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study date",
                            color = Color(0xFF686F7A),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = selectedDateMillis?.let {
                                dateFormatter.format(Date(it))
                            } ?: "No date selected",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true }
                    ) {
                        Text("Choose")
                    }
                }
            }
        }

        // Time selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Study session",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeCard(
                    title = "START TIME",
                    time = String.format(
                        Locale.ROOT, "%02d:%02d",
                        startHour, startMinute
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        choosingStartTime = true
                        showTimePicker = true
                    }
                )

                TimeCard(
                    title = "END TIME",
                    time = String.format(
                        Locale.ROOT, "%02d:%02d",
                        endHour, endMinute
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        choosingStartTime = false
                        showTimePicker = true
                    }
                )
            }

            Text(
                text = "Choose a start and end time for your visit.",
                fontSize = 13.sp,
                color = Color(0xFF686F7A)
            )
        }

        // Session summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5EE)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reservation summary",
                    fontWeight = FontWeight.SemiBold,
                    color = SeatSpotGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Seat S$seatNumber")
                Text(
                    text = selectedDateMillis?.let {
                        dateFormatter.format(Date(it))
                    } ?: "Select a date to continue"
                )
                Text(
                    text = String.format(
                        Locale.ROOT, "%02d:%02d - %02d:%02d",
                        startHour, startMinute, endHour, endMinute
                    )
                )
            }
        }

        val validTime =
            startHour * 60 + startMinute < endHour * 60 + endMinute

        Button(
            onClick = {
                val selectedDate = selectedDateMillis?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(it))
                }

                if (selectedDate != null && validTime) {
                    val start = String.format(
                        Locale.ROOT, "%02d:%02d",
                        startHour, startMinute
                    )
                    val end = String.format(
                        Locale.ROOT, "%02d:%02d",
                        endHour, endMinute
                    )
                    onConfirmClick(selectedDate, "$start-$end")
                }
            },
            enabled = selectedDateMillis != null && validTime,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Confirm Reservation",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Seats")
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis =
                            datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text(
                        "Select",
                        color = SeatSpotBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(
                        "Cancel",
                        color = SeatSpotBlue
                    )
                }
            }
        ) {

            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = SeatSpotBlue,
                    headlineContentColor = SeatSpotBlue,
                    selectedDayContainerColor = SeatSpotBlue,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = SeatSpotBlue,
                    todayContentColor = SeatSpotBlue,
                    selectedYearContainerColor = SeatSpotBlue,
                    selectedYearContentColor = Color.White
                )
            )

        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = if (choosingStartTime) startHour else endHour,
            initialMinute = if (choosingStartTime) startMinute else endMinute,
            is24Hour = true
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    if (choosingStartTime) "Choose start time"
                    else "Choose end time"
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (choosingStartTime) {
                            startHour = timePickerState.hour
                            startMinute = timePickerState.minute
                        } else {
                            endHour = timePickerState.hour
                            endMinute = timePickerState.minute
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("Set time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TimeCard(
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF686F7A),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = time,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SeatSpotBlue
            )
            Text(
                text = "Tap to change",
                fontSize = 11.sp,
                color = Color(0xFF686F7A)
            )
        }
    }
}
