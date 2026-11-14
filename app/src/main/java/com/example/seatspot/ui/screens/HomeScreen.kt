package com.example.seatspot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.model.Library
import com.example.seatspot.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlinx.coroutines.launch
import com.example.seatspot.ui.components.SeatSpotBottomBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.seatspot.viewmodel.MyReservationsViewModel
import com.example.seatspot.data.model.MyReservation
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.seatspot.R

private val DeepIndigo = Color(0xFF1B2A6B)
private val RoyalBlue = Color(0xFF2447A8)
private val Teal = Color(0xFF1FB5A8)
private val Mint = Color(0xFF7FE8D8)
private val ScreenBackground = Color(0xFFF3F6FC)
private val PrimaryText = Color(0xFF172554)
private val SecondaryText = Color(0xFF64748B)
private val SoftBlue = Color(0xFFE8EEFF)
private val SoftTeal = Color(0xFFDDF7F2)
private val SoftIndigo = Color(0xFFE4E7F6)

@Composable
fun HomeScreen(
    onExploreSeatsClick: () -> Unit,
    onMyReservationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDiscussionRoomsClick: () -> Unit,
    reservationsViewModel: MyReservationsViewModel,
    onAiAssistantClick: () -> Unit
){
    val reservationsUiState by reservationsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            currentUser.getIdToken(false)
                .addOnSuccessListener { result ->
                    result.token?.let { token ->
                        reservationsViewModel.loadReservations(token)
                    }
                }
        }
    }
    val userName = FirebaseAuth.getInstance()
        .currentUser?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Student"

    val libraryState = remember { mutableStateOf<Library?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val loadError = remember { mutableStateOf<String?>(null) }

    val firestoreRepository = remember {
        FirestoreRepository()
    }

    DisposableEffect(Unit) {
        val listener = firestoreRepository.observeLibrary { result ->

            result.onSuccess { library ->
                libraryState.value = library
                loadError.value = null
                isLoading.value = false
            }

            result.onFailure { exception ->
                loadError.value = exception.localizedMessage
                    ?: "Unable to load library information."
                isLoading.value = false
            }
        }

        onDispose {
            listener.remove()
        }
    }

    val initials = userName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val overviewTop = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {

            // HEADER
            DashboardHeader(
                greeting = greeting,
                userName = userName,
                initials = initials.ifEmpty { "S" }
            )

            // CONTENT THAT OVERLAPS THE HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ){

                // LIBRARY OVERVIEW
                // FIND YOUR FOCUS ZONE
                FocusZoneBanner()
                AiStudyAssistantCard(
                    onClick = onAiAssistantClick
                )

                // UPCOMING BOOKING (no reservation data is available on this screen)
                val upcomingReservation = remember(reservationsUiState.reservations) {
                    val now = System.currentTimeMillis()
                    val today = java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.US
                    ).format(java.util.Date(now))

                    reservationsUiState.reservations
                        .filter { reservation ->
                            val status = reservation.status.uppercase()

                            if (status == "CANCELLED" ||
                                status == "CANCELED" ||
                                status == "COMPLETED"
                            ) {
                                false
                            } else {
                                val endMillis = parseReservationEndTime(reservation.endAt)

                                if (endMillis != null) {
                                    endMillis >= now
                                } else {
                                    reservation.date.isNotBlank() &&
                                            reservation.date >= today
                                }
                            }
                        }
                        .sortedBy { it.startAt ?: "" }
                        .firstOrNull()
                }

                when {
                    reservationsUiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = RoyalBlue
                        )
                    }

                    reservationsUiState.error != null -> {
                        Text(
                            text = "Unable to load your bookings.",
                            color = Color(0xFFB42318),
                            fontSize = 13.sp
                        )
                    }

                    upcomingReservation != null -> {
                        UpcomingBookingCard(
                            reservation = upcomingReservation,
                            onClick = onMyReservationsClick
                        )
                    }

                    else -> {
                        UpcomingBookingEmptyState(
                            onBookClick = onExploreSeatsClick
                        )
                    }
                }

                // QUICK ACTIONS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    SectionTitle("Quick actions")

                    // First row: Original three options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAction(
                            kind = IconKind.SEAT,
                            title = "Find a Seat",
                            tileColor = SoftBlue,
                            iconColor = RoyalBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onExploreSeatsClick
                        )

                        QuickAction(
                            kind = IconKind.CALENDAR,
                            title = "My Bookings",
                            tileColor = SoftTeal,
                            iconColor = Color(0xFF0F8C80),
                            modifier = Modifier.weight(1f),
                            onClick = onMyReservationsClick
                        )

                        QuickAction(
                            kind = IconKind.LIBRARY,
                            title = "Explore Library",
                            tileColor = SoftIndigo,
                            iconColor = DeepIndigo,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    scrollState.animateScrollTo(
                                        (overviewTop.value - 24).coerceAtLeast(0)
                                    )
                                }
                            }
                        )
                    }

                    // DISCUSSION ROOMS PROMOTIONAL SECTION
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionTitle("Study together")

                        DiscussionRoomPromoCard(
                            onClick = onDiscussionRoomsClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // BOTTOM NAVIGATION
        SeatSpotBottomBar(
            selectedRoute = "home",
            onDestinationClick = { route ->
                when (route) {
                    "seatmap" -> onExploreSeatsClick()
                    "my_reservations" -> onMyReservationsClick()
                    "profile" -> onProfileClick()
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// HEADER
// ---------------------------------------------------------------------------

@Composable
private fun DashboardHeader(
    greeting: String,
    userName: String,
    initials: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepIndigo, RoyalBlue)
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.98f, size.height * 0.05f)
                )
                drawCircle(
                    color = Teal.copy(alpha = 0.22f),
                    radius = size.width * 0.30f,
                    center = Offset(size.width * 0.04f, size.height * 1.02f)
                )
            }
            .padding(start = 22.dp, end = 22.dp, top = 38.dp, bottom = 66.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    color = Mint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Welcome back. Ready for a focused study session?",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Mint)
                    .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = DeepIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LIBRARY OVERVIEW
// ---------------------------------------------------------------------------

@Composable
private fun LibraryOverviewCard(
    isLoading: Boolean,
    error: String?,
    library: Library?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = DeepIndigo.copy(alpha = 0.18f),
                spotColor = DeepIndigo.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(Color.White)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(listOf(RoyalBlue, Teal))
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(IconKind.LIBRARY, Color.White, 26.dp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Central Library",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Library overview",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = RoyalBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading library information...",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }

            error != null -> {
                Text(
                    text = "Unable to load library information",
                    color = Color(0xFFB42318),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            library != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        value = "${library.totalResources}",
                        label = "Study resources",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ScreenBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(RoyalBlue, Teal)))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = value,
                color = PrimaryText,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// UPCOMING BOOKING
// ---------------------------------------------------------------------------

@Composable
private fun UpcomingBookingEmptyState(onBookClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(DeepIndigo, RoyalBlue))
            )
            .drawBehind {
                drawCircle(
                    color = Teal.copy(alpha = 0.28f),
                    radius = size.height * 0.55f,
                    center = Offset(size.width * 0.96f, size.height * 0.1f)
                )
            }
            .padding(20.dp)
    ) {
        Text(
            text = "UPCOMING BOOKING",
            color = Mint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(IconKind.SEAT, Color.White, 30.dp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No upcoming booking",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Reserve a seat for your next study session.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBookClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = RoyalBlue
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Book a Seat",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppIcon(IconKind.ARROW, RoyalBlue, 18.dp)
        }
    }
}
//@Composable
//private fun UpcomingBookingCard(
//    reservation: MyReservation,
//    onClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(26.dp))
//            .background(
//                Brush.linearGradient(listOf(DeepIndigo, RoyalBlue))
//            )
//            .padding(20.dp)
//    ) {
//        Text(
//            text = "UPCOMING BOOKING",
//            color = Mint,
//            fontSize = 11.sp,
//            fontWeight = FontWeight.Bold,
//            letterSpacing = 1.2.sp
//        )
//
//        Spacer(modifier = Modifier.height(14.dp))
//
//        Text(
//            text = when (reservation.resourceType.uppercase()) {
//                "COMPUTER" -> "Computer Station"
//                "DISCUSSION_ROOM" -> "Discussion Room"
//                else -> "Study Seat"
//            },
//            color = Color.White,
//            fontSize = 19.sp,
//            fontWeight = FontWeight.Bold
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = "Resource: ${reservation.resourceId}",
//            color = Color.White.copy(alpha = 0.85f),
//            fontSize = 13.sp
//        )
//
//        Text(
//            text = "Date: ${reservation.date}",
//            color = Color.White.copy(alpha = 0.85f),
//            fontSize = 13.sp
//        )
//
//        Text(
//            text = "Time: ${formatReservationTime(reservation.startAt)} – ${
//                formatReservationTime(reservation.endAt)
//            }",
//            color = Color.White.copy(alpha = 0.85f),
//            fontSize = 13.sp
//        )
//
//        Spacer(modifier = Modifier.height(18.dp))
//
//        Button(
//            onClick = onClick,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(48.dp),
//            shape = RoundedCornerShape(16.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Color.White,
//                contentColor = RoyalBlue
//            )
//        ) {
//            Text(
//                text = "View All Bookings",
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}


@Composable
private fun UpcomingBookingCard(
    reservation: MyReservation,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1B2A6B),
                        Color(0xFF2447A8)
                    )
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "UPCOMING BOOKING",
            color = Mint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp
        )

        Text(
            text = when (reservation.resourceType.uppercase()) {
                "COMPUTER" -> "Computer Station"
                "DISCUSSION_ROOM" -> "Discussion Room"
                else -> "Study Seat"
            },
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BookingInfoTile(
                label = "Resource",
                value = reservation.resourceId,
                modifier = Modifier.weight(1f)
            )

            BookingInfoTile(
                label = "Date",
                value = reservation.date,
                modifier = Modifier.weight(1f)
            )

            BookingInfoTile(
                label = "Time",
                value = "${formatReservationTime(reservation.startAt)} – ${
                    formatReservationTime(reservation.endAt)
                }",
                modifier = Modifier.weight(1.35f)
            )
        }

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = RoyalBlue
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp
            )
        ) {
            Text(
                text = "View All Bookings",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BookingInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 10.sp,
            maxLines = 1
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ---------------------------------------------------------------------------
// QUICK ACTIONS
// ---------------------------------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun QuickAction(
    kind: IconKind,
    title: String,
    tileColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = DeepIndigo.copy(alpha = 0.10f),
                spotColor = DeepIndigo.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(tileColor.copy(alpha = 0.65f))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(kind, iconColor, 26.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun DiscussionRoomPromoCard(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF11998E),
                        Color(0xFF087F75)
                    )
                )
            )
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFDDF7F2)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    kind = IconKind.GROUP,
                    tint = Color(0xFF087F75),
                    size = 26.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Better together",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Group project or study circle?",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bring your ideas together! Reserve a private discussion room and collaborate without disturbing anyone.",
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF087F75)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 3.dp
            )
        ) {
            Text(
                text = "Reserve a Room  →",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ---------------------------------------------------------------------------
// VECTOR-FREE ICONS (drawn on a Canvas, so no icon dependency is needed)
// ---------------------------------------------------------------------------

private enum class IconKind {
    HOME, SEAT, CALENDAR, PROFILE, LIBRARY, ARROW, GROUP
}

@Composable
private fun AppIcon(
    kind: IconKind,
    tint: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = w * 0.09f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (kind) {
            IconKind.GROUP -> {
                // Centre person
                drawCircle(
                    color = tint,
                    radius = w * 0.105f,
                    center = Offset(w * 0.5f, h * 0.29f),
                    style = stroke
                )

                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.29f, h * 0.48f),
                    size = Size(w * 0.42f, h * 0.40f),
                    style = stroke
                )

                // Left person
                drawCircle(
                    color = tint,
                    radius = w * 0.075f,
                    center = Offset(w * 0.19f, h * 0.40f),
                    style = stroke
                )

                drawArc(
                    color = tint,
                    startAngle = 190f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(w * 0.04f, h * 0.60f),
                    size = Size(w * 0.30f, h * 0.27f),
                    style = stroke
                )

                // Right person
                drawCircle(
                    color = tint,
                    radius = w * 0.075f,
                    center = Offset(w * 0.81f, h * 0.40f),
                    style = stroke
                )

                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(w * 0.66f, h * 0.60f),
                    size = Size(w * 0.30f, h * 0.27f),
                    style = stroke
                )
            }
            IconKind.HOME -> {
                val roof = Path().apply {
                    moveTo(w * 0.12f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.88f, h * 0.48f)
                }
                val body = Path().apply {
                    moveTo(w * 0.22f, h * 0.42f)
                    lineTo(w * 0.22f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.42f)
                }
                drawPath(roof, tint, style = stroke)
                drawPath(body, tint, style = stroke)
            }

            IconKind.SEAT -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.3f, h * 0.1f),
                    size = Size(w * 0.4f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.52f),
                    size = Size(w * 0.64f, h * 0.17f),
                    cornerRadius = CornerRadius(w * 0.07f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.3f, h * 0.69f), Offset(w * 0.3f, h * 0.9f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.7f, h * 0.69f), Offset(w * 0.7f, h * 0.9f), stroke.width, StrokeCap.Round)
            }

            IconKind.CALENDAR -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.2f),
                    size = Size(w * 0.72f, h * 0.66f),
                    cornerRadius = CornerRadius(w * 0.12f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.14f, h * 0.42f), Offset(w * 0.86f, h * 0.42f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.1f), Offset(w * 0.34f, h * 0.28f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.1f), Offset(w * 0.66f, h * 0.28f), stroke.width, StrokeCap.Round)
            }

            IconKind.PROFILE -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.17f,
                    center = Offset(w * 0.5f, h * 0.32f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.56f),
                    size = Size(w * 0.6f, h * 0.6f),
                    style = stroke
                )
            }

            IconKind.LIBRARY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(w * 0.2f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.4f, h * 0.18f),
                    size = Size(w * 0.2f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.05f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.68f, h * 0.24f), Offset(w * 0.86f, h * 0.82f), stroke.width, StrokeCap.Round)
            }

            IconKind.ARROW -> {
                drawLine(tint, Offset(w * 0.16f, h * 0.5f), Offset(w * 0.84f, h * 0.5f), stroke.width, StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.56f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.5f)
                    lineTo(w * 0.56f, h * 0.78f)
                }
                drawPath(head, tint, style = stroke)
            }
        }
    }
}

private fun parseReservationEndTime(value: String?): Long? {
    if (value.isNullOrBlank()) return null

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    for (pattern in patterns) {
        try {
            val format = java.text.SimpleDateFormat(
                pattern,
                java.util.Locale.US
            ).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }

            return format.parse(value)?.time
        } catch (_: Exception) {
            // Try the next format.
        }
    }

    return null
}

private fun formatReservationTime(value: String?): String {
    if (value.isNullOrBlank()) return "--"

    return try {
        val inputFormat = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            java.util.Locale.US
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        val date = inputFormat.parse(value)
            ?: return value

        java.text.SimpleDateFormat(
            "hh:mm a",
            java.util.Locale.getDefault()
        ).format(date)
    } catch (_: Exception) {
        value
    }
}


@Composable
private fun FocusZoneBanner() {
    Image(
        painter = painterResource(
            id = R.drawable.focus_zone_banner
        ),
        contentDescription = "Find Your Focus Zone",
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp).padding(top=12.dp)
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Crop
    )
}


@Composable
private fun AiStudyAssistantCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(SoftTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✦",
                        color = Teal,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Study Assistant",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart recommendations for you",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "NEW",
                    color = RoyalBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Find a study space that matches your needs.",
                color = SecondaryText,
                fontSize = 13.sp
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Explore AI Recommendations",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
