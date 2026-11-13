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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.model.Library
import com.example.seatspot.data.repository.FirestoreRepository
import com.example.seatspot.getFirebaseIdToken
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlinx.coroutines.launch
import com.example.seatspot.ui.components.SeatSpotBottomBar

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
    onDiscussionRoomsClick: () -> Unit

) {
    LaunchedEffect(Unit) {
        getFirebaseIdToken()
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
                    .layout { measurable, constraints ->
                        val overlap = 34.dp.roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height - overlap) {
                            placeable.place(0, -overlap)
                        }
                    }
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {

                // LIBRARY OVERVIEW
                LibraryOverviewCard(
                    isLoading = isLoading.value,
                    error = loadError.value,
                    library = libraryState.value,
                    modifier = Modifier.onGloballyPositioned {
                        overviewTop.value = it.positionInParent().y.toInt()
                    }
                )

                // UPCOMING BOOKING (no reservation data is available on this screen)
                UpcomingBookingEmptyState(onBookClick = onExploreSeatsClick)

                // QUICK ACTIONS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Quick actions")

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

                        QuickAction(
                            kind = IconKind.LIBRARY,
                            title = "Discussion Rooms",
                            tileColor = SoftIndigo,
                            iconColor = DeepIndigo,
                            modifier = Modifier.fillMaxWidth(),
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
            .background(Color.White)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tileColor),
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

// ---------------------------------------------------------------------------
// VECTOR-FREE ICONS (drawn on a Canvas, so no icon dependency is needed)
// ---------------------------------------------------------------------------

private enum class IconKind { HOME, SEAT, CALENDAR, PROFILE, LIBRARY, ARROW }

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