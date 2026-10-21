package com.example.seatspot.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profile = UserProfileChangeRequest
                        .Builder()
                        .setDisplayName(name.trim())
                        .build()

                    auth.currentUser?.updateProfile(profile)
                        ?.addOnCompleteListener {
                            onResult(true, null)
                        } ?: onResult(true, null)
                } else {
                    onResult(
                        false,
                        task.exception?.localizedMessage ?: "Registration failed."
                    )
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(
                        false,
                        task.exception?.localizedMessage ?: "Login failed."
                    )
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}