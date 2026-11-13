package com.example.seatspot.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.seatspot.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
object GoogleSignInHelper {

    suspend fun signIn(context: Context): Boolean {
        val credentialManager = CredentialManager.create(context)

        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(
                context.getString(R.string.default_web_client_id)
            )
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = try {
            credentialManager.getCredential(
                context = context,
                request = request
            )
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {

            val signInWithGoogleOption =
                com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
                    .Builder(
                        context.getString(R.string.default_web_client_id)
                    )
                    .build()

            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            credentialManager.getCredential(
                context = context,
                request = fallbackRequest
            )
        }

        val credential = result.credential

        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google sign-in returned an unexpected credential.")
        }

        val googleCredential =
            GoogleIdTokenCredential.createFrom(credential.data)

        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        return suspendCancellableCoroutine { continuation ->
            FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener

                    if (task.isSuccessful) {
                        continuation.resume(true)
                    } else {
                        continuation.resumeWithException(
                            task.exception
                                ?: Exception("Firebase Google sign-in failed.")
                        )
                    }
                }
        }
    }
}
