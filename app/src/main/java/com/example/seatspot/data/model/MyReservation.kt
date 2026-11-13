
package com.example.seatspot.data.model

data class MyReservation(
    val reservationId: String = "",
    val userId: String = "",
    val libraryId: String = "",
    val resourceType: String = "",
    val resourceId: String = "",
    val floorId: String = "",
    val areaId: String = "",
    val sectionId: String = "",
    val tableId: String = "",
    val labId: String = "",
    val date: String = "",
    val startAt: String? = null,
    val endAt: String? = null,
    val durationMinutes: Int = 0,
    val memberCount: Int = 1,
    val status: String = "",
    val createdAt: String? = null
)
