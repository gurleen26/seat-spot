package com.example.seatspot.data.model

import org.json.JSONObject
import java.util.UUID

data class AiOption(
    val resourceType: String = "",
    val resourceId: String = "",
    val floorId: String = "",
    val areaId: String = "",
    val sectionId: String = "",
    val tableId: String = "",
    val labId: String = "",
    val seatNumber: Int = 0,
    val label: String = "",
    val capacity: Int = 0,
    val defaultDurationMinutes: Int = 60
)

data class AiProposal(
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val options: List<AiOption> = emptyList(),
    val groupSize: Int = 0,
    val durationMinutes: Int = 60
)

data class AiBookingResponse(
    val success: Boolean = false,
    val understood: Boolean = false,
    val message: String = "",
    val proposal: AiProposal? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val proposal: AiProposal? = null,
    val isTyping: Boolean = false
)


object AiBookingParser {
    fun parseResponse(jsonString: String): Result<AiBookingResponse> {
        return try {
            val json = JSONObject(jsonString)
            val success = json.optBoolean("success", false)
            val understood = json.optBoolean("understood", false)
            val message = json.optString("message", "")

            val proposal = if (json.has("proposal") && !json.isNull("proposal")) {
                val propObj = json.optJSONObject("proposal")

                if (propObj != null) {
                    val date = propObj.optString("date", "")
                    val startTime = propObj.optString("startTime", "")
                    val endTime = propObj.optString("endTime", "")

                    val groupSize = propObj.optInt("groupSize", 0)
                    val durationMinutes = propObj.optInt("durationMinutes", 60)

                    val optionsArray = propObj.optJSONArray("options")
                    val optionsList = mutableListOf<AiOption>()

                    if (optionsArray != null) {
                        for (i in 0 until optionsArray.length()) {
                            val optObj = optionsArray.optJSONObject(i) ?: continue

                            optionsList.add(
                                AiOption(
                                    resourceType = optObj.optString("resourceType", ""),
                                    resourceId = optObj.optString("resourceId", ""),
                                    floorId = optObj.optString("floorId", ""),
                                    areaId = optObj.optString("areaId", ""),
                                    sectionId = optObj.optString("sectionId", ""),
                                    tableId = optObj.optString("tableId", ""),
                                    labId = optObj.optString("labId", ""),
                                    seatNumber = optObj.optInt("seatNumber", 0),
                                    label = optObj.optString("label", ""),
                                    capacity = optObj.optInt("capacity", 0),
                                    defaultDurationMinutes = optObj.optInt(
                                        "defaultDurationMinutes", 60
                                    )
                                )
                            )
                        }
                    }

                    AiProposal(
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        options = optionsList,
                        groupSize = groupSize,
                        durationMinutes = durationMinutes
                    )
                } else null
            } else null

            Result.success(
                AiBookingResponse(
                    success = success,
                    understood = understood,
                    message = message,
                    proposal = proposal
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
