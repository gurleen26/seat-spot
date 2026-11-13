package com.example.seatspot.data.model

data class ComputerStation(
    val computerId: String = "",
    val computerNumber: Int = 0,

    // Location information
    val floorId: String = "first",
    val labId: String = "",

    // Computer information
    val computerType: String = "WINDOWS",
    val operatingSystem: String = "",
    val specifications: String = "",

    // Whether this computer can be booked
    val isActive: Boolean = true
)
