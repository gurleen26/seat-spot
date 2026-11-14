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



    val resourceLabel = when (resourceType) {
        "COMPUTER" -> "Computer"
        "SECOND_FLOOR_SEAT" -> "Table seat"
        else -> "Study seat"
    }
    val resourceCode = if (resourceType == "COMPUTER") "PC $seatNumber" else "S$seatNumber"
    val locationDescription = when (resourceType) {
        "COMPUTER" -> "First Floor • $labId"
        "SECOND_FLOOR_SEAT" -> "Second Floor • $sectionId • $tableId"
        else -> "Ground Floor • $areaId • $sectionId"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeatSpotBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = SeatSpotBlue,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "Review booking",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172B67)
                )
                Text(
                    text = "Make sure everything looks right",
                    color = Color(0xFF687386),
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF172B67))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = "YOUR STUDY SPACE",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Central Library",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = locationDescription,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 13.sp
                        )
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF20B6A6))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = resourceCode,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = resourceLabel,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Text(
                    text = if (resourceType == "COMPUTER") "Computer ID: $seatId" else "Resource ID: $seatId",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Reservation details",
                color = Color(0xFF172B67),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DATE", color = Color(0xFF8490A3), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(displayDate, color = Color(0xFF1D2B49), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DURATION", color = Color(0xFF8490A3), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            val durationMinutes = runCatching {
                                val a = startTime.split(":")
                                val b = endTime.split(":")
                                (b[0].toInt() * 60 + b[1].toInt()) - (a[0].toInt() * 60 + a[1].toInt())
                            }.getOrDefault(0)
                            Text(
                                text = if (durationMinutes > 0) "${durationMinutes / 60}h ${durationMinutes % 60}m" else "—",
                                color = Color(0xFF20A596),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFFE9EEF5))
                    Column {
                        Text("STUDY TIME", color = Color(0xFF8490A3), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "$startTime  –  $endTime",
                            color = Color(0xFF2447A8),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F6F2))
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓", color = Color(0xFF168A78), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.padding(start = 11.dp)) {
                    Text("Ready to reserve?", color = Color(0xFF146B60), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Your selected study space will be held for this time slot.", color = Color(0xFF47766F), fontSize = 12.sp)
                }
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
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2447A8),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF9AA9C9),
                disabledContentColor = Color.White
            )
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bookingSuccess) Color(0xFFE5F6F2) else Color(0xFFFFEEEE)
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(15.dp),
                    color = if (bookingSuccess) SeatSpotGreen else Color(0xFFB3261E),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (resourceType == "COMPUTER") "←  Edit computer selection" else "←  Edit seat selection",
                color = Color(0xFF2447A8),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
