package com.example.seatspot.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.model.DiscussionRoom
import com.example.seatspot.network.SeatSpotApi
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val BookingBlue = Color(0xFF2447A8)
private val BookingNavy = Color(0xFF172B67)
private val BookingTeal = Color(0xFF20B6B0)
private val BookingBg = Color(0xFFF2F5FC)
private val BookingMuted = Color(0xFF64748B)
private val BookingBorder = Color(0xFFE3E9F4)
private val BookingGreen = Color(0xFF23834B)

@Composable
fun DiscussionRoomBookingScreen(
    room: DiscussionRoom,
    onBackClick: () -> Unit,
    onBookClick: (String, String, Int, Int) -> Unit,
    initialDate: String = "",
    initialStartTime: String = "",
    initialDuration: Int = 60,
    initialGroupSize: Int = 2
) {
    var groupSize by remember(room.roomId) {
        mutableIntStateOf(initialGroupSize.coerceIn(2, 6))
    }

    val context = LocalContext.current

    var selectedDate by remember(room.roomId) {
        mutableStateOf(initialDate)
    }
    var duration by remember(room.roomId) {
        mutableIntStateOf(
            if (initialDuration in listOf(60, 90, 120)) initialDuration else 60
        )
    }
    var selectedStartTime by remember(room.roomId) {
        mutableStateOf(initialStartTime)
    }
    var reservedSlots by remember {
        mutableStateOf<List<Pair<Long, Long>>>(emptyList())
    }

    var isCheckingAvailability by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDate, room.roomId) {
        if (selectedDate.isNotEmpty()) {
            isCheckingAvailability = true
            availabilityError = null
            reservedSlots = emptyList()

            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                availabilityError = "Please sign in again."
                isCheckingAvailability = false
            } else {
                user.getIdToken(false)
                    .addOnSuccessListener { tokenResult ->
                        SeatSpotApi.getDiscussionRoomAvailability(
                            idToken = tokenResult.token!!,
                            date = selectedDate,
                            roomId = room.roomId
                        ) { result ->
                            result.onSuccess { response ->
                                try {
                                    val json = JSONObject(response)
                                    val slots = json.getJSONArray("reservedSlots")
                                    val parsedSlots =
                                        mutableListOf<Pair<Long, Long>>()

                                    val formatter = SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                        Locale.US
                                    ).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }

                                    for (index in 0 until slots.length()) {
                                        val item = slots.getJSONObject(index)

                                        val startMillis = formatter.parse(
                                            item.getString("startAt")
                                        )!!.time

                                        val endMillis = formatter.parse(
                                            item.getString("endAt")
                                        )!!.time

                                        parsedSlots.add(
                                            Pair(startMillis, endMillis)
                                        )
                                    }

                                    reservedSlots = parsedSlots
                                } catch (e: Exception) {
                                    availabilityError =
                                        "Unable to read availability."
                                }

                                isCheckingAvailability = false
                            }

                            result.onFailure { error ->
                                availabilityError = error.message
                                    ?: "Unable to check availability."
                                isCheckingAvailability = false
                            }
                        }
                    }
                    .addOnFailureListener {
                        availabilityError =
                            "Unable to authenticate. Please sign in again."
                        isCheckingAvailability = false
                    }
            }
        }
    }

    val isPastTime = remember(selectedDate, selectedStartTime) {
        if (selectedDate.isEmpty() || selectedStartTime.isEmpty()) {
            false
        } else {
            try {
                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }

                val selectedMillis =
                    formatter.parse("$selectedDate $selectedStartTime")!!.time

                selectedMillis <= System.currentTimeMillis()
            } catch (e: Exception) {
                false
            }
        }
    }

    val isSlotAvailable = remember(
        selectedDate,
        selectedStartTime,
        duration,
        reservedSlots
    ) {
        if (selectedDate.isEmpty() || selectedStartTime.isEmpty()) {
            false
        } else {
            try {
                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }

                val startMillis = formatter.parse(
                    "$selectedDate $selectedStartTime"
                )!!.time

                val endMillis = startMillis + duration * 60_000L

                reservedSlots.none { slot ->
                    startMillis < slot.second &&
                            endMillis > slot.first
                }
            } catch (e: Exception) {
                availabilityError =
                    "Unable to check the selected time. Please try again."
                false
            }
        }
    }

    val timeSlots = listOf(
        "09:00", "10:00", "11:00",
        "12:00", "13:00", "14:00",
        "15:00", "16:00", "17:00"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BookingBg)
    ) {

        // Indigo header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    BookingBlue,
                    RoundedCornerShape(
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    )
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Text(
                text = "‹  Back",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onBackClick() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = room.roomName,
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Reserve your group study session",
                color = Color(0xFFDCE7FF),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF4262B5),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "CAPACITY  ${room.capacity} students",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Surface(
                    color = Color(0xFF4262B5),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "60–120 min",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {

            // Date selection
            BookingSectionTitle("01", "Choose booking date")

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
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
                            selectedStartTime = ""
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BookingBlue
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BookingBorder
                )
            ) {
                Text(
                    text = if (selectedDate.isEmpty()) {
                        "Select booking date"
                    } else {
                        selectedDate
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Duration
            BookingSectionTitle("02", "Select duration")

            Spacer(modifier = Modifier.height(10.dp))

            BookingWhiteCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60, 90, 120).forEach { minutes ->
                        BookingChoiceChip(
                            text = "$minutes min",
                            selected = duration == minutes,
                            modifier = Modifier.weight(1f)
                        ) {
                            duration = minutes
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Time slots
            BookingSectionTitle("03", "Choose start time")

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Select a time for your reservation",
                color = BookingMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            BookingWhiteCard {
                if (selectedDate.isEmpty()) {
                    Text(
                        text = "Please select a booking date first.",
                        color = BookingMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        timeSlots.chunked(3).forEach { rowSlots ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSlots.forEach { time ->
                                    BookingChoiceChip(
                                        text = time,
                                        selected = selectedStartTime == time,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        selectedStartTime = time
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Group size
            BookingSectionTitle("04", "Group size")

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Choose the number of students (2–6)",
                color = BookingMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            BookingWhiteCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    (2..6).forEach { size ->
                        BookingChoiceChip(
                            text = "$size",
                            selected = groupSize == size,
                            modifier = Modifier.weight(1f)
                        ) {
                            groupSize = size
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Availability feedback
            when {
                isCheckingAvailability -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BookingBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Checking room availability...",
                            color = BookingMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                availabilityError != null -> {
                    BookingFeedback(
                        text = availabilityError!!,
                        color = Color(0xFFB3261E)
                    )
                }

                isPastTime -> {
                    BookingFeedback(
                        text = "This time has already passed. Choose a future time.",
                        color = Color(0xFFB3261E)
                    )
                }

                selectedStartTime.isNotEmpty() && isSlotAvailable -> {
                    BookingFeedback(
                        text = "Selected time is available.",
                        color = BookingGreen
                    )
                }

                selectedStartTime.isNotEmpty() -> {
                    BookingFeedback(
                        text = "This time slot is already booked. Choose another.",
                        color = Color(0xFFB3261E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onBookClick(
                        selectedDate,
                        selectedStartTime,
                        duration,
                        groupSize
                    )
                },
                enabled = selectedDate.isNotEmpty() &&
                        selectedStartTime.isNotEmpty() &&
                        !isPastTime &&
                        isSlotAvailable &&
                        !isCheckingAvailability &&
                        availabilityError == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BookingBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFB8C5E0),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BookingSectionTitle(
    number: String,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFFE3EAFF),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = number,
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 6.dp
                ),
                color = BookingBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = BookingNavy,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BookingWhiteCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BookingBorder,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            content = content
        )
    }
}

@Composable
private fun BookingChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(43.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(11.dp),
        color = if (selected) BookingBlue else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) BookingBlue else BookingBorder
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else BookingNavy,
                fontSize = 13.sp,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
            )
        }
    }
}

@Composable
private fun BookingFeedback(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
