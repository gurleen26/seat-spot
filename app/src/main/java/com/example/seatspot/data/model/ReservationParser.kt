package com.example.seatspot.data.model

import org.json.JSONArray
import org.json.JSONObject

object ReservationParser {

    fun parse(json: String): List<MyReservation> {
        val root = JSONObject(json)
        val array: JSONArray = root.optJSONArray("reservations")
            ?: return emptyList()

        val reservations = mutableListOf<MyReservation>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)

            reservations.add(
                MyReservation(
                    reservationId = item.optString("reservationId"),
                    userId = item.optString("userId"),
                    libraryId = item.optString("libraryId"),
                    resourceType = item.optString("resourceType"),
                    resourceId = item.optString("resourceId"),
                    floorId = item.optString("floorId"),
                    areaId = item.optString("areaId"),
                    sectionId = item.optString("sectionId"),
                    tableId = item.optString("tableId"),
                    labId = item.optString("labId"),
                    date = item.optString("date"),
                    startAt = item.optString("startAt").takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    endAt = item.optString("endAt").takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    durationMinutes = item.optInt("durationMinutes"),
                    memberCount = item.optInt("memberCount", 1),
                    status = item.optString("status"),
                    createdAt = item.optString("createdAt").takeIf {
                        it.isNotBlank() && it != "null"
                    }
                )
            )
        }

        return reservations
    }
}
