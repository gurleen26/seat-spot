package com.example.seatspot.data.model

import com.google.firebase.Timestamp

data class Reservation(
    val reservationId: String = "",
    val userId: String = "",
    val libraryId: String = "central_library",

    // Resource information
    // SEAT, COMPUTER or DISCUSSION_ROOM
    val resourceType: String = "",
    val resourceId: String = "",

    // Resource location
    val floorId: String = "",
    val areaId: String = "",
    val sectionId: String = "",
    val tableId: String = "",
    val labId: String = "",

    // Booking date and time
    val date: String = "",
    val startAt: Timestamp? = null,
    val endAt: Timestamp? = null,
    val durationMinutes: Int = 60,

    // Discussion room group information
    val memberCount: Int = 1,
    val memberIds: List<String> = emptyList(),

    // PENDING, CONFIRMED, CANCELLED, COMPLETED
    val status: String = "PENDING",

    val createdAt: Timestamp? = null
)
