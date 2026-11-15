
package com.example.seatspot.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.seatspot.data.repository.AuthRepository
import com.example.seatspot.network.SeatSpotApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


@Composable
fun DeleteAccountScreen(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val user = FirebaseAuth.getInstance().currentUser

    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFinalConfirmation by remember { mutableStateOf(false) }

    val isGoogleUser = user?.providerData?.any {
        it.providerId == GoogleAuthProvider.PROVIDER_ID
    } == true

    val isEmailUser = user?.providerData?.any {
        it.providerId == "password"
    } == true

    fun deleteAccountAfterReauthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            isLoading = false
            errorMessage = "Please sign in again."
            return
        }

        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token

                if (token.isNullOrBlank()) {
                    isLoading = false
                    errorMessage = "Unable to get authentication token."
                    return@addOnSuccessListener
                }

                SeatSpotApi.deleteAccount(token) { result ->
                    isLoading = false

                    result.onSuccess {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(
                            context,
                            "Your account has been deleted.",
                            Toast.LENGTH_LONG
                        ).show()
                        onAccountDeleted()
                    }

                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Account deletion failed."
                    }
                }
            }
            .addOnFailureListener {
                isLoading = false
                errorMessage = it.localizedMessage
                    ?: "Unable to authenticate user."
            }
    }

    fun reauthenticateAndDelete() {
        isLoading = true
        errorMessage = null

        if (isEmailUser) {
            authRepository.reauthenticateWithPassword(password) { success, error ->
                if (success) {
                    deleteAccountAfterReauthentication()
                } else {
                    isLoading = false
                    errorMessage = error
                }
            }
        } else if (isGoogleUser) {
            authRepository.reauthenticateWithGoogle(context) { success, error ->
                if (success) {
                    deleteAccountAfterReauthentication()
                } else {
                    isLoading = false
                    errorMessage = error
                }
            }
        } else {
            isLoading = false
            errorMessage = "This sign-in method is not supported yet."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("← Back")
        }

        Text(
            text = "Delete Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "For your security, please verify your identity before deleting your SeatSpot account.",
            style = MaterialTheme.typography.bodyMedium
        )

        when {
            isEmailUser -> {
                Text("Enter your account password")

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            isGoogleUser -> {
                Text(
                    "You signed in using Google. Continue to verify your Google account."
                )
            }

            else -> {
                Text(
                    "Your current sign-in method is not supported for account deletion."
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { showFinalConfirmation = true },
            enabled = !isLoading &&
                    (isGoogleUser || (isEmailUser && password.isNotBlank())),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Text("Continue to Delete Account")
            }
        }
    }

    if (showFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinalConfirmation = false },
            title = { Text("Permanently Delete Account?") },
            text = {
                Text(
                    "This action cannot be undone. Your future reservations " +
                            "will be cancelled and your account will be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalConfirmation = false
                        reauthenticateAndDelete()
                    }
                ) {
                    Text(
                        "Delete Permanently",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
