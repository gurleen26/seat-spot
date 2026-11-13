package com.example.seatspot.data.model

data class DiscussionRoom(
    val roomId: String = "",
    val roomName: String = "",

    // Physical location (to be configured later)
    val floorId: String = "",
    val areaId: String = "",
    val sectionId: String = "",

    // Booking configuration
    val capacity: Int = 8,
    val defaultDurationMinutes: Int = 60,

    // Room facilities
    val features: List<String> = emptyList(),

    // Whether the room can be booked
    val isActive: Boolean = true
)
