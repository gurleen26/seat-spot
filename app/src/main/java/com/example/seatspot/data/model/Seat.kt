package com.example.seatspot.data.model

data class Seat(
    val seatId: String = "",
    val seatNumber: Int = 0,

    // Location information
    val floorId: String = "",
    val areaId: String = "",
    val sectionId: String = "",
    val tableId: String = "",

    // Seat classification
    val seatType: String = "GENERAL",
    val features: List<String> = emptyList(),

    // Whether this seat is available for booking
    val isActive: Boolean = true
)
