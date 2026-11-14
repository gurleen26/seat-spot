package com.example.seatspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.ui.components.SeatSpotBottomBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

private val ProfileIndigo = Color(0xFF1B2A6B)
private val ProfileBlue = Color(0xFF2447A8)
private val ProfileTeal = Color(0xFF1FB5A8)
private val ProfileBackground = Color(0xFFF3F6FC)
private val ProfileText = Color(0xFF172554)
private val ProfileSecondary = Color(0xFF64748B)
private val ProfileDanger = Color(0xFFD32F2F)

@Composable
fun ProfileScreen(
    onMyReservationsClick: () -> Unit,
    onLogout: () -> Unit,
    onTabClick: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    var displayName by remember(user?.uid) {
        mutableStateOf(user?.displayName.orEmpty())
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(displayName) }
    var message by remember { mutableStateOf<String?>(null) }

    val email = user?.email ?: "Email unavailable"
    val initials = displayName.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "S" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // PROFILE HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ProfileIndigo,
                                ProfileBlue,
                                ProfileTeal
                            )
                        ),
                        shape = RoundedCornerShape(
                            bottomStart = 30.dp,
                            bottomEnd = 30.dp
                        )
                    )
                    .padding(top = 38.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "My Profile",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ProfileTeal, ProfileBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = ProfileIndigo,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayName.ifBlank { "Student" },
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = email,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileSectionTitle("Account")

                ProfileOption(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Change your display name",
                    onClick = {
                        editName = displayName
                        showEditDialog = true
                    }
                )

                ProfileOption(
                    icon = Icons.Default.CalendarMonth,
                    title = "My Reservations",
                    subtitle = "View your booking history",
                    onClick = onMyReservationsClick
                )

                ProfileOption(
                    icon = Icons.Default.Security,
                    title = "Account & Security",
                    subtitle = if (user?.isEmailVerified == true) {
                        "Email verified"
                    } else {
                        "Email verification status"
                    },
                    onClick = {
                        message = "Signed in with: ${
                            user?.providerData
                                ?.mapNotNull { it.providerId }
                                ?.filter { it != "firebase" }
                                ?.joinToString()
                                ?.ifBlank { "Email/Password" }
                                ?: "Email/Password"
                        }"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProfileSectionTitle("Preferences & Support")

                ProfileOption(
                    icon = Icons.Default.NotificationsNone,
                    title = "Notifications",
                    subtitle = "Notification preferences",
                    onClick = {
                        message = "Notification settings will be available in a future update."
                    }
                )

                ProfileOption(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & Support",
                    subtitle = "Get assistance with SeatSpot",
                    onClick = {
                        message = "For support, please contact your library administrator."
                    }
                )

                ProfileOption(
                    icon = Icons.Default.Info,
                    title = "About SeatSpot",
                    subtitle = "App information",
                    onClick = {
                        message = "SeatSpot\nLibrary Seat Reservation App\nVersion 1.0"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProfileOption(
                    icon = Icons.Default.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    isDestructive = true,
                    onClick = { showLogoutDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        SeatSpotBottomBar(
            selectedRoute = "profile",
            onDestinationClick = { route ->
                onTabClick(route)
            }
        )
    }

    // EDIT PROFILE DIALOG
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Profile",
                    color = ProfileText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfileBlue,
                        focusedLabelColor = ProfileBlue,
                        cursorColor = ProfileBlue
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = editName.trim()
                        if (newName.isNotBlank()) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build()

                            user?.updateProfile(profileUpdates)
                                ?.addOnSuccessListener {
                                    displayName = newName
                                    message = "Profile name updated."
                                }
                                ?.addOnFailureListener {
                                    message = it.localizedMessage
                                        ?: "Unable to update profile."
                                }
                        } else {
                            message = "Name cannot be empty."
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ProfileBlue
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ProfileSecondary
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // LOGOUT CONFIRMATION
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    color = ProfileText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to sign out of SeatSpot?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ProfileDanger
                    )
                ) {
                    Text("Logout", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ProfileBlue
                    )
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // INFORMATION MESSAGE
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = {
                Text(
                    text = "SeatSpot",
                    color = ProfileText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(text) },
            confirmButton = {
                TextButton(
                    onClick = { message = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ProfileBlue
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        color = ProfileText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val itemColor = if (isDestructive) ProfileDanger else ProfileText
    val iconBackground = if (isDestructive) {
        Color(0xFFFFEEEE)
    } else {
        Color(0xFFE8EEFF)
    }
    val iconTint = if (isDestructive) ProfileDanger else ProfileBlue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = itemColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color = ProfileSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ProfileSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
