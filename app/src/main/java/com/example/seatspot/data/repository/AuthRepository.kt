package com.example.seatspot.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import android.content.Context
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.example.seatspot.data.GoogleSignInHelper
import kotlinx.coroutines.launch

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

    fun reauthenticateWithPassword(
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            onResult(false, "No authenticated user found.")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(
                        false,
                        task.exception?.localizedMessage
                            ?: "Reauthentication failed."
                    )
                }
            }
    }

    fun reauthenticateWithGoogle(
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(false, "No authenticated user found.")
            return
        }

        val isGoogleUser = user.providerData.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        }

        if (!isGoogleUser) {
            onResult(false, "This account is not linked to Google.")
            return
        }

        kotlinx.coroutines.MainScope().launch {
            try {
                val credential =
                    GoogleSignInHelper.getFreshGoogleCredential(context)

                user.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onResult(true, null)
                        } else {
                            onResult(
                                false,
                                task.exception?.localizedMessage
                                    ?: "Google reauthentication failed."
                            )
                        }
                    }
            } catch (e: Exception) {
                onResult(
                    false,
                    e.localizedMessage ?: "Google reauthentication failed."
                )
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}