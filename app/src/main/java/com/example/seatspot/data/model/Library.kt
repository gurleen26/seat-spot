
package com.example.seatspot.data.model

data class Library(
    val libraryId: String = "central_library",
    val name: String = "Central Library",
    val location: String = "",
    val totalFloors: Int = 3,
    val totalSeats: Int = 780,
    val totalComputers: Int = 90,
    val totalDiscussionRooms: Int = 2,
    val isActive: Boolean = true
) {
    val totalResources: Int
        get() = totalSeats + totalComputers
}
