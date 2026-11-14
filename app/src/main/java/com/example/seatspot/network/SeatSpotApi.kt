
package com.example.seatspot.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object SeatSpotApi {

    const val BASE_URL = "https://unclamped-walnut-stipend.ngrok-free.dev"

    private val client = OkHttpClient()

    fun checkAuthentication(
        idToken: String,
        onResult: (Result<String>) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/api/me")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                onResult(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string().orEmpty()

                    if (it.isSuccessful) {
                        onResult(Result.success(responseBody))
                    } else {
                        onResult(
                            Result.failure(
                                Exception("HTTP ${it.code}: $responseBody")
                            )
                        )
                    }
                }
            }
        })
    }

    fun getMyReservations(
        idToken: String,
        onResult: (Result<String>) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/api/reservations")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(responseBody))
                        } else {
                            val message = try {
                                JSONObject(responseBody).optString(
                                    "message",
                                    "Unable to fetch reservations."
                                )
                            } catch (e: Exception) {
                                "HTTP ${res.code}: Unable to fetch reservations."
                            }

                            onResult(Result.failure(Exception(message)))
                        }
                    }
                }
            }
        })
    }



    fun getSeatAvailability(
        idToken: String,
        date: String,
        floorId: String,
        areaId: String = "",
        sectionId: String = "",
        labId: String = "",
        tableId: String = "",
        onResult: (Result<String>) -> Unit
    ) {
        val url = buildString {
            append("$BASE_URL/api/availability")
            append("?date=$date")
            append("&floorId=$floorId")

            if (floorId == "ground") {
                append("&areaId=$areaId")
                append("&sectionId=$sectionId")
            } else if (floorId == "first") {
                append("&labId=$labId")
            } else if (floorId == "second") {
                append("&sectionId=$sectionId")
                append("&tableId=$tableId")
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(responseBody))
                        } else {
                            onResult(
                                Result.failure(
                                    Exception("HTTP ${res.code}: $responseBody")
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    fun createReservation(
        idToken: String,
        resourceId: String,
        floorId: String,
        areaId: String = "",
        sectionId: String = "",
        date: String,
        startTime: String,
        endTime: String,
        resourceType: String = "SEAT",
        labId: String = "",
        tableId: String = "",
        onResult: (Result<String>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("resourceType", resourceType)
            put("resourceId", resourceId)
            put("floorId", floorId)

            if (resourceType == "SEAT") {
                put("areaId", areaId)
                put("sectionId", sectionId)

            } else if (resourceType == "COMPUTER") {
                put("labId", labId)

            } else if (resourceType == "SECOND_FLOOR_SEAT") {
                put("sectionId", sectionId)
                put("tableId", tableId)
            }

            put("date", date)
            put("startTime", startTime)
            put("endTime", endTime)
        }

        val requestBody = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url("$BASE_URL/api/reservations")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(responseBody))
                        } else {
                            val message = try {
                                JSONObject(responseBody).optString(
                                    "message",
                                    "Reservation failed."
                                )
                            } catch (e: Exception) {
                                "HTTP ${res.code}: Reservation failed."
                            }

                            onResult(Result.failure(Exception(message)))
                        }
                    }
                }
            }
        })
    }

    fun getDiscussionRoomAvailability(
        idToken: String,
        date: String,
        roomId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val url = "$BASE_URL/api/availability" +
                "?date=$date" +
                "&resourceType=DISCUSSION_ROOM" +
                "&roomId=$roomId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val body = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(body))
                        } else {
                            onResult(
                                Result.failure(
                                    Exception("HTTP ${res.code}: $body")
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    fun createDiscussionRoomReservation(
        idToken: String,
        roomId: String,
        memberCount: Int,
        date: String,
        startTime: String,
        endTime: String,
        onResult: (Result<String>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("resourceType", "DISCUSSION_ROOM")
            put("resourceId", roomId)
            put("roomId", roomId)
            put("memberCount", memberCount)
            put("date", date)
            put("startTime", startTime)
            put("endTime", endTime)
        }

        val requestBody = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url("$BASE_URL/api/reservations")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val body = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(body))
                        } else {
                            onResult(
                                Result.failure(
                                    Exception("HTTP ${res.code}: $body")
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    fun cancelReservation(
        idToken: String,
        reservationId: String,
        onResult: (Result<String>) -> Unit
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/api/reservations/$reservationId")
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("ngrok-skip-browser-warning", "true")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onResult(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string().orEmpty()

                    Handler(Looper.getMainLooper()).post {
                        if (res.isSuccessful) {
                            onResult(Result.success(responseBody))
                        } else {
                            val message = try {
                                JSONObject(responseBody).optString(
                                    "message",
                                    "Unable to cancel reservation."
                                )
                            } catch (e: Exception) {
                                "HTTP ${res.code}: Unable to cancel reservation."
                            }

                            onResult(Result.failure(Exception(message)))
                        }
                    }
                }
            }
        })
    }


}
