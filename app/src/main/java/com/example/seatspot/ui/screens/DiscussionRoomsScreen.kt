
package com.example.seatspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seatspot.data.model.DiscussionRoom
import com.example.seatspot.data.repository.DiscussionRoomRepository

@Composable
fun DiscussionRoomsScreen(
    onBackClick: () -> Unit,
    onRoomClick: (DiscussionRoom) -> Unit
) {
    val repository = remember { DiscussionRoomRepository() }

    var rooms by remember { mutableStateOf<List<DiscussionRoom>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadRooms() {
        isLoading = true
        errorMessage = null

        repository.getDiscussionRooms { result ->
            result.onSuccess { roomList ->
                rooms = roomList.filter { it.isActive }
                isLoading = false
            }

            result.onFailure { error ->
                errorMessage = error.localizedMessage
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
            .padding(20.dp)
    ) {
        Text(
            text = "Discussion Rooms",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reserve a room for your group study session.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = { loadRooms() }) {
                    Text("Retry")
                }
            }

            rooms.isEmpty() -> {
                Text("No discussion rooms are currently available.")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = rooms,
                        key = { it.roomId }
                    ) { room ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 3.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    text = room.roomName,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Capacity: ${room.capacity} students")
                                Text("Default duration: ${room.defaultDurationMinutes} minutes")

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { onRoomClick(room) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Book This Room")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
