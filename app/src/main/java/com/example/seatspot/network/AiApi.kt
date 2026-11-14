package com.example.seatspot.network

import android.os.Handler
import android.os.Looper
import com.example.seatspot.data.model.AiBookingParser
import com.example.seatspot.data.model.AiBookingResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun parseBooking(
        idToken: String,
        text: String,
        onResult: (Result<AiBookingResponse>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("text", text)
        }

        val requestBody = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url("${SeatSpotApi.BASE_URL}/api/ai/parse-booking")
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
                            val parseResult = AiBookingParser.parseResponse(responseBody)
                            if (parseResult.isSuccess) {
                                onResult(parseResult)
                            } else {
                                onResult(
                                    Result.failure(
                                        Exception("HTTP ${res.code}: Unable to parse response.")
                                    )
                                )
                            }
                        } else {
                            val message = try {
                                JSONObject(responseBody).optString("message", "")
                            } catch (e: Exception) {
                                ""
                            }

                            val errorMsg = if (message.isNotBlank()) {
                                "HTTP ${res.code}: $message"
                            } else {
                                "HTTP ${res.code}"
                            }

                            onResult(Result.failure(Exception(errorMsg)))
                        }
                    }
                }
            }
        })
    }
}
