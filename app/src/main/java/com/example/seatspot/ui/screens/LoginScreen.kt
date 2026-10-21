
package com.example.seatspot.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.AuthRepository
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.example.seatspot.R

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val authRepository = AuthRepository()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Log in to find your study seat.")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val cleanEmail = email.trim()

                when {
                    cleanEmail.isBlank() ->
                        errorMessage = "Please enter your email."

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(cleanEmail).matches() ->
                        errorMessage = "Please enter a valid email address."

                    password.isBlank() ->
                        errorMessage = "Please enter your password."

                    else -> {
                        isLoading = true
                        errorMessage = null

                        authRepository.login(
                            email = cleanEmail,
                            password = password
                        ) { success, error ->
                            isLoading = false

                            if (success) {
                                onLoginClick()
                            } else {
                                    errorMessage = getFriendlyLoginError(error)
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Log In")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                // Google Sign-In will be connected later
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text("Continue with Google")
            }
        }

        TextButton(
            onClick = onRegisterClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Register")
        }
    }
}



private fun getFriendlyLoginError(error: String?): String {
    return when {
        error.isNullOrBlank() ->
            "Something went wrong. Please try again."

        error.contains("no user record", ignoreCase = true) ||
                error.contains("user not found", ignoreCase = true) ->
            "No account found with this email. Please register first."

        error.contains("wrong password", ignoreCase = true) ||
                error.contains("invalid credential", ignoreCase = true) ||
                error.contains("invalid login credentials", ignoreCase = true) ||
                error.contains("malformed", ignoreCase = true) ->
            "Unable to log in. Check your email and password, or register for an account."

        error.contains("too many requests", ignoreCase = true) ->
            "Too many attempts. Please wait a while and try again."

        error.contains("network", ignoreCase = true) ->
            "Please check your internet connection and try again."

        error.contains("user disabled", ignoreCase = true) ->
            "This account is disabled. Please contact support."

        else ->
            "We couldn't log you in. Please try again."
    }
}
