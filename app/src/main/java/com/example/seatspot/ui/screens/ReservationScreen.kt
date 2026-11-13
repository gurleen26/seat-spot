
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.auth.FirebaseAuth
import com.example.seatspot.network.SeatSpotApi
import java.text.ParseException
import androidx.compose.material3.TextButton

private val SeatSpotBlue = Color(0xFF2447A8)
private val SeatSpotBackground = Color(0xFFF5F7FA)
private val SeatSpotGreen = Color(0xFF18794E)

@Composable
fun ReservationScreen(
    seatId: String,
    seatNumber: Int,
    areaId: String,
    sectionId: String,
    tableId: String = "",
    reservationDate: String,
    startTime: String,
    endTime: String,
    resourceType: String = "SEAT",
    labId: String = "",
    onBackClick: () -> Unit
) {
    var isBooking by remember { mutableStateOf(false) }
    var bookingMessage by remember { mutableStateOf<String?>(null) }
    var bookingSuccess by remember { mutableStateOf(false) }

    val displayDate = remember(reservationDate) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(reservationDate)?.let { date ->
            SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(date)
        } ?: reservationDate
    }

    val validTime = runCatching {
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")

        val startMinutes =
            startParts[0].toInt() * 60 + startParts[1].toInt()

        val endMinutes =
            endParts[0].toInt() * 60 + endParts[1].toInt()

        startMinutes < endMinutes
    }.getOrDefault(false)



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
            text = when (resourceType) {
                "COMPUTER" -> "Reserve Your Computer"
                "SECOND_FLOOR_SEAT" -> "Reserve Your Table Seat"
                else -> "Reserve Your Seat"
            },
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
                        text = if (resourceType == "COMPUTER") {
                            "Computer ID: $seatId"
                        } else {
                            seatId
                        },
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
                            text = if (resourceType == "COMPUTER") {
                                "PC $seatNumber"
                            } else {
                                "S$seatNumber"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SeatSpotBlue
                        )
                        Text(
                            if (resourceType == "COMPUTER") "Your computer" else "Your seat",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Date selection
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your study session",
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "RESERVATION DATE",
                        fontSize = 12.sp,
                        color = Color(0xFF686F7A)
                    )

                    Text(
                        text = displayDate,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "STUDY TIMING",
                        fontSize = 12.sp,
                        color = Color(0xFF686F7A)
                    )

                    Text(
                        text = "$startTime - $endTime",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SeatSpotBlue
                    )
                }
            }
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

                Text(
                    when (resourceType) {
                        "COMPUTER" -> "Computer PC $seatNumber"
                        "SECOND_FLOOR_SEAT" ->
                            "Second Floor • $sectionId • $tableId • Seat S$seatNumber"
                        else -> "Seat S$seatNumber"
                    }
                )
                Text(displayDate)
                Text("$startTime - $endTime")
            }
        }

        Button(

            onClick = {
                // Reject past reservations using India local time.
                val dateTimeFormatter = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.ROOT
                ).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                    isLenient = false
                }

                val startDateTime = try {
                    dateTimeFormatter.parse("$reservationDate $startTime")
                } catch (e: ParseException) {
                    null
                }

                if (
                    startDateTime == null ||
                    startDateTime.time <= System.currentTimeMillis()
                ) {
                    bookingMessage = "Please choose a future start time."
                    return@Button
                }

                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser == null) {
                    bookingMessage = "Your session has expired. Please sign in again."
                    return@Button
                }

                isBooking = true
                bookingMessage = null

                currentUser.getIdToken(false)
                    .addOnSuccessListener { tokenResult ->

                        val idToken = tokenResult.token

                        if (idToken.isNullOrBlank()) {
                            isBooking = false
                            bookingMessage = "Unable to authenticate your request."
                            return@addOnSuccessListener
                        }

                        SeatSpotApi.createReservation(
                            idToken = idToken,
                            resourceId = seatId,
                            floorId = when (resourceType) {
                                "COMPUTER" -> "first"
                                "SECOND_FLOOR_SEAT" -> "second"
                                else -> "ground"
                            },
                            areaId = areaId,
                            sectionId = sectionId,
                            date = reservationDate,
                            startTime = startTime,
                            endTime = endTime,
                            resourceType = resourceType,
                            labId = labId,
                            tableId = tableId
                        ) { result ->

                            isBooking = false

                            result.onSuccess {
                                bookingSuccess = true
                                bookingMessage = when (resourceType) {
                                    "COMPUTER" -> "Computer reserved successfully!"
                                    "SECOND_FLOOR_SEAT" -> "Second Floor seat reserved successfully!"
                                    else -> "Seat reserved successfully!"
                                }
                            }

                            result.onFailure { error ->
                                bookingSuccess = false
                                bookingMessage = error.localizedMessage
                                    ?: "Unable to complete reservation."
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        isBooking = false
                        bookingMessage = error.localizedMessage
                            ?: "Authentication failed. Please try again."
                    }
            },
            enabled = validTime &&
                    !isBooking &&
                    !bookingSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isBooking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (bookingSuccess) "Reservation Confirmed"
                    else "Confirm Reservation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        bookingMessage?.let { message ->
            Text(
                text = message,
                color = if (bookingSuccess) {
                    SeatSpotGreen
                } else {
                    Color.Red
                },
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (resourceType == "COMPUTER") {
                    "Back to Computers"
                } else {
                    "Back to Seats"
                }
            )
        }
    }
}

