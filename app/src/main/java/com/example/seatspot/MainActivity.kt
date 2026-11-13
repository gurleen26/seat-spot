
package com.example.seatspot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.seatspot.navigation.SeatSpotNavigation
import com.example.seatspot.network.SeatSpotApi
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            SeatSpotNavigation()
        }
    }
}

fun getFirebaseIdToken() {

    val user = FirebaseAuth.getInstance().currentUser

    if (user == null) {
        Log.e("SeatSpotAPI", "No user is logged in")
        return
    }

    user.getIdToken(false)
        .addOnSuccessListener { result ->

            val idToken = result.token

            if (idToken == null) {
                Log.e("SeatSpotAPI", "Firebase ID token is null")
                return@addOnSuccessListener
            }

            Log.d("SeatSpotAPI", "Firebase ID token retrieved successfully")

            SeatSpotApi.checkAuthentication(idToken) { response ->

                response
                    .onSuccess { body ->
                        Log.d("SeatSpotAPI", "API Response: $body")
                    }
                    .onFailure { error ->
                        Log.e("SeatSpotAPI", "API Error: ${error.message}")
                    }
            }
        }
        .addOnFailureListener { exception ->
            Log.e("SeatSpotAPI", "Token error: ${exception.message}")
        }
}
