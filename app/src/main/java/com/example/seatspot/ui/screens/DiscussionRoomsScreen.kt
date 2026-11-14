
package com.example.seatspot.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.R
import com.example.seatspot.data.model.DiscussionRoom
import com.example.seatspot.data.repository.DiscussionRoomRepository

private val PrimaryBlue = Color(0xFF2447A8)
private val DarkNavy = Color(0xFF172B67)
private val TealAccent = Color(0xFF20B6B0)
private val ScreenBg = Color(0xFFF2F5FC)
private val MutedText = Color(0xFF64748B)
private val BorderColor = Color(0xFFE5EAF4)
private val GreenText = Color(0xFF23834B)

@Composable
fun DiscussionRoomsScreen(
    onBackClick: () -> Unit,
    onRoomClick: (DiscussionRoom) -> Unit
) {
    val repository = remember { DiscussionRoomRepository() }

    var rooms by remember {
        mutableStateOf<List<DiscussionRoom>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadRooms() {
        isLoading = true
        errorMessage = null

        repository.getDiscussionRooms { result ->
            result.onSuccess { roomList ->
                rooms = roomList
                    .filter { it.isActive }
                    .distinctBy { it.roomId }
                    .sortedBy { it.roomId }
                isLoading = false
            }

            result.onFailure {
                errorMessage = it.localizedMessage
                    ?: "Unable to load discussion rooms."
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRooms()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onBackClick,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text(
                text = "‹  Back",
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Text(
            text = "Discussion Rooms",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DarkNavy
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Reserve a room for your group study session.",
            fontSize = 14.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Keep the existing illustration
        Image(
            painter = painterResource(R.drawable.discussion_room_banner),
            contentDescription = "Students collaborating in a discussion room",
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )

                TextButton(onClick = { loadRooms() }) {
                    Text("Retry", color = PrimaryBlue)
                }
            }

            rooms.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No discussion rooms available.",
                        color = MutedText
                    )
                }
            }

            else -> {

                // Room count heading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE ROOMS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = PrimaryBlue
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(TealAccent)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        color = Color(0xFFE7EDFF),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${rooms.size} Rooms",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(
                        items = rooms,
                        key = { it.roomId }
                    ) { room ->

                        val roomNumber = room.roomId
                            .substringAfterLast("_")
                            .toIntOrNull()

                        val displayName = if (roomNumber != null) {
                            "Discussion Room $roomNumber"
                        } else {
                            room.roomName
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = BorderColor,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                // Room name and active status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayName,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy
                                    )

                                    Surface(
                                        color = Color(0xFFE6F6ED),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        GreenText,
                                                        CircleShape
                                                    )
                                            )

                                            Spacer(
                                                modifier = Modifier.width(5.dp)
                                            )

                                            Text(
                                                text = "Active",
                                                color = GreenText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(13.dp))

                                HorizontalDivider(color = BorderColor)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Capacity and duration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Groups,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(19.dp)
                                        )

                                        Spacer(modifier = Modifier.width(7.dp))

                                        Text(
                                            text = "${room.capacity} students",
                                            fontSize = 13.sp,
                                            color = MutedText
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = "${room.defaultDurationMinutes} min",
                                            fontSize = 13.sp,
                                            color = MutedText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(15.dp))

                                // Booking button
                                Button(
                                    onClick = { onRoomClick(room) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 2.dp
                                    )
                                ) {
                                    Text(
                                        text = "Book This Room",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
