package com.example.seatspot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.model.MyReservation
import com.example.seatspot.viewmodel.MyReservationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.seatspot.ui.components.SeatSpotBottomBar

// ---------------------------------------------------------------------------
// PALETTE (shared with HomeScreen)
// ---------------------------------------------------------------------------

private val DeepIndigo = Color(0xFF1B2A6B)
private val RoyalBlue = Color(0xFF2447A8)
private val Teal = Color(0xFF1FB5A8)
private val Mint = Color(0xFF7FE8D8)
private val ScreenBackground = Color(0xFFF3F6FC)
private val PrimaryText = Color(0xFF172554)
private val SecondaryText = Color(0xFF64748B)
private val SoftBlue = Color(0xFFE8EEFF)
private val SoftTeal = Color(0xFFDDF7F2)

// Status colours
private val ConfirmedColor = Color(0xFF0F8C80)
private val ConfirmedSoft = SoftTeal
private val PendingColor = Color(0xFFB7791F)
private val PendingSoft = Color(0xFFFFF4DB)
private val CompletedColor = RoyalBlue
private val CompletedSoft = SoftBlue
private val CancelledColor = Color(0xFFB42318)
private val CancelledSoft = Color(0xFFFDECEA)

// ---------------------------------------------------------------------------
// TABS
// ---------------------------------------------------------------------------

private enum class BookingTab(val label: String) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

// ---------------------------------------------------------------------------
// SCREEN
// ---------------------------------------------------------------------------

@Composable
fun MyReservationsScreen(
    idToken: String,
    onBackClick: () -> Unit,
    viewModel: MyReservationsViewModel,
    onTabClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(idToken) {
        viewModel.loadReservations(idToken)
    }

    var selectedTabName by rememberSaveable { mutableStateOf(BookingTab.UPCOMING.name) }
    val selectedTab = BookingTab.valueOf(selectedTabName)
    var reservationToCancel by remember {
        mutableStateOf<MyReservation?>(null)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.cancellationError, uiState.cancellationSuccess) {
        val message = uiState.cancellationError ?: uiState.cancellationSuccess

        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearCancellationMessages()
        }
    }

    // Group the already-loaded reservations into the three tabs.
    val grouped = remember(uiState.reservations) {
        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        uiState.reservations
            .groupBy { bucketOf(it, now, today) }
    }

    val upcoming = remember(grouped) {
        grouped[BookingTab.UPCOMING].orEmpty().sortedBy { it.startAt ?: "" }
    }
    val completed = remember(grouped) {
        grouped[BookingTab.COMPLETED].orEmpty().sortedByDescending { it.startAt ?: "" }
    }
    val cancelled = remember(grouped) {
        grouped[BookingTab.CANCELLED].orEmpty().sortedByDescending { it.startAt ?: "" }
    }

    val visible = when (selectedTab) {
        BookingTab.UPCOMING -> upcoming
        BookingTab.COMPLETED -> completed
        BookingTab.CANCELLED -> cancelled
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        BookingsHeader(
            onBackClick = onBackClick,
            onRefreshClick = { viewModel.loadReservations(idToken) },
            refreshEnabled = !uiState.isLoading
        )

        // Tab bar overlaps the header, like the dashboard content does.
        TabBar(
            selected = selectedTab,
            counts = mapOf(
                BookingTab.UPCOMING to upcoming.size,
                BookingTab.COMPLETED to completed.size,
                BookingTab.CANCELLED to cancelled.size
            ),
            onSelect = { selectedTabName = it.name },
            modifier = Modifier
                .layout { measurable, constraints ->
                    val overlap = 30.dp.roundToPx()
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height - overlap) {
                        placeable.place(0, -overlap)
                    }
                }
                .padding(horizontal = 20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = RoyalBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading your bookings...",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )
                    }
                }

                uiState.error != null -> {
                    StateCard(
                        title = "Couldn't load your bookings",
                        message = uiState.error ?: "Something went wrong.",
                        titleColor = CancelledColor,
                        actionLabel = "Try Again",
                        onAction = { viewModel.loadReservations(idToken) }
                    )
                }

                uiState.reservations.isEmpty() -> {
                    StateCard(
                        title = "No bookings yet",
                        message = "Your library bookings will appear here."
                    )
                }

                visible.isEmpty() -> {
                    StateCard(
                        title = when (selectedTab) {
                            BookingTab.UPCOMING -> "No upcoming bookings"
                            BookingTab.COMPLETED -> "No completed bookings"
                            BookingTab.CANCELLED -> "No cancelled bookings"
                        },
                        message = when (selectedTab) {
                            BookingTab.UPCOMING -> "Bookings you make will show up here."
                            BookingTab.COMPLETED -> "Bookings you've attended will show up here."
                            BookingTab.CANCELLED -> "Bookings you cancel will show up here."
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = visible,
                            key = { it.reservationId }
                        ) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                bucket = selectedTab,
                                isCancelling = uiState.cancellingReservationId == reservation.reservationId,
                                onCancelClick = {
                                    reservationToCancel = reservation
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
        SeatSpotBottomBar(
            selectedRoute = "my_reservations",
            onDestinationClick = { route ->
                onTabClick(route)
            }
        )
    }
    reservationToCancel?.let { reservation ->
        AlertDialog(
            onDismissRequest = {
                if (uiState.cancellingReservationId == null) {
                    reservationToCancel = null
                }
            },
            title = {
                Text("Cancel this booking?")
            },
            text = {
                Text(
                    "Are you sure you want to cancel this reservation? " +
                            "The selected time slot will become available again."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = uiState.cancellingReservationId == null,
                    onClick = {
                        viewModel.cancelReservation(
                            idToken = idToken,
                            reservationId = reservation.reservationId
                        )
                        reservationToCancel = null
                    }
                ) {
                    Text(
                        if (uiState.cancellingReservationId == reservation.reservationId) {
                            "Cancelling..."
                        } else {
                            "Yes, cancel"
                        },
                        color = CancelledColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = uiState.cancellingReservationId == null,
                    onClick = {
                        reservationToCancel = null
                    }
                ) {
                    Text("Keep booking")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// HEADER
// ---------------------------------------------------------------------------

@Composable
private fun BookingsHeader(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    refreshEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(Brush.linearGradient(colors = listOf(DeepIndigo, RoyalBlue)))
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
            .padding(start = 22.dp, end = 22.dp, top = 38.dp, bottom = 58.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeaderPill(
                    text = "‹  Back",
                    enabled = true,
                    onClick = onBackClick
                )

                HeaderPill(
                    text = "Refresh",
                    enabled = refreshEnabled,
                    onClick = onRefreshClick
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "My Bookings",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "View your library bookings",
                color = Mint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeaderPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.08f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// TABS
// ---------------------------------------------------------------------------

@Composable
private fun TabBar(
    selected: BookingTab,
    counts: Map<BookingTab, Int>,
    onSelect: (BookingTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)

    Row(
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
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BookingTab.values().forEach { tab ->
            val isSelected = tab == selected

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(listOf(DeepIndigo, RoyalBlue))
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val count = counts[tab] ?: 0
                if (count > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Teal else SoftBlue)
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count",
                            color = if (isSelected) Color.White else RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LOADING / EMPTY / ERROR CARD
// ---------------------------------------------------------------------------

@Composable
private fun StateCard(
    title: String,
    message: String,
    titleColor: Color = PrimaryText,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = DeepIndigo.copy(alpha = 0.18f),
                    spotColor = DeepIndigo.copy(alpha = 0.22f)
                )
                .clip(shape)
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(RoyalBlue, Teal))),
                contentAlignment = Alignment.Center
            ) {
                CalendarGlyph(Color.White, 30.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = titleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// BOOKING CARD
// ---------------------------------------------------------------------------

@Composable
private fun ReservationCard(
    reservation: MyReservation,
    bucket: BookingTab,
    isCancelling: Boolean,
    onCancelClick: () -> Unit
){
    val floorName = when (reservation.floorId.lowercase()) {
        "ground" -> "Ground Floor"
        "first" -> "First Floor"
        "second" -> "Second Floor"
        else -> reservation.floorId.replaceFirstChar { it.uppercase() }
    }

    val resourceName = when (reservation.resourceType.uppercase()) {
        "SEAT" -> "Study Seat"
        "COMPUTER" -> "Computer Station"
        "DISCUSSION_ROOM" -> "Discussion Room"
        else -> reservation.resourceType.replace("_", " ")
            .replaceFirstChar { it.uppercase() }
    }

    // Status indicator: Completed/Cancelled follow the tab the booking sits in;
    // Upcoming bookings show their real status (Confirmed / Pending / ...).
    val statusLabel: String
    val statusColor: Color
    val statusSoft: Color

    when (bucket) {
        BookingTab.CANCELLED -> {
            statusLabel = "Cancelled"
            statusColor = CancelledColor
            statusSoft = CancelledSoft
        }

        BookingTab.COMPLETED -> {
            statusLabel = "Completed"
            statusColor = CompletedColor
            statusSoft = CompletedSoft
        }

        BookingTab.UPCOMING -> {
            when (reservation.status.uppercase()) {
                "PENDING" -> {
                    statusLabel = "Pending"
                    statusColor = PendingColor
                    statusSoft = PendingSoft
                }

                "CONFIRMED" -> {
                    statusLabel = "Confirmed"
                    statusColor = ConfirmedColor
                    statusSoft = ConfirmedSoft
                }

                else -> {
                    statusLabel = reservation.status
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                    statusColor = ConfirmedColor
                    statusSoft = ConfirmedSoft
                }
            }
        }
    }

    val details = buildList {
        if (reservation.resourceId.isNotBlank()) add("Resource ID" to reservation.resourceId)
        add("Floor" to floorName)
        if (reservation.areaId.isNotBlank()) add("Area" to reservation.areaId)
        if (reservation.sectionId.isNotBlank()) add("Section" to reservation.sectionId)
        if (reservation.tableId.isNotBlank()) add("Table" to reservation.tableId)
    }

    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = DeepIndigo.copy(alpha = 0.14f),
                spotColor = DeepIndigo.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(Color.White)
            .padding(18.dp)
    ) {
        // TITLE ROW
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (bucket == BookingTab.CANCELLED) {
                            Brush.linearGradient(
                                listOf(SecondaryText.copy(alpha = 0.7f), SecondaryText)
                            )
                        } else {
                            Brush.linearGradient(listOf(RoyalBlue, Teal))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                ResourceGlyph(reservation.resourceType, Color.White, 26.dp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resourceName,
                    color = PrimaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = floorName,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusChip(
                label = statusLabel,
                color = statusColor,
                background = statusSoft
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LOCATION DETAILS
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            details.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { (label, value) ->
                        DetailItem(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DATE / TIME / DURATION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ScreenBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(RoyalBlue, Teal)))
            )

            Spacer(modifier = Modifier.width(14.dp))

            DetailItem(
                label = "Date",
                value = formatDate(reservation.date),
                modifier = Modifier.weight(1.15f)
            )

            DetailItem(
                label = "Time",
                value = "${formatTime(reservation.startAt)} - ${formatTime(reservation.endAt)}",
                modifier = Modifier.weight(1.5f)
            )


            DetailItem(
                label = "Duration",
                value = formatDuration(reservation.durationMinutes.toString()),
                modifier = Modifier.weight(0.9f)
            )
        }

        // CANCEL BOOKING BUTTON
        if (
            bucket == BookingTab.UPCOMING &&
            (reservation.status.equals("CONFIRMED", ignoreCase = true) ||
                    reservation.status.equals("PENDING", ignoreCase = true)) &&
            (parseIsoMillis(reservation.startAt) ?: 0L) > System.currentTimeMillis()
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onCancelClick,
                enabled = !isCancelling,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CancelledColor
                )
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = CancelledColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelling...")
                } else {
                    Text(
                        "Cancel Booking",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
private fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    background: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// ICONS (Canvas-drawn, no icon dependency, same approach as HomeScreen)
// ---------------------------------------------------------------------------

@Composable
private fun ResourceGlyph(resourceType: String, tint: Color, size: Dp) {
    val type = resourceType.uppercase()

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)

        when (type) {
            "COMPUTER" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.5f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.5f, h * 0.84f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.86f), Offset(w * 0.7f, h * 0.86f), stroke.width, StrokeCap.Round)
            }

            "DISCUSSION_ROOM" -> {
                drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.34f, h * 0.34f), style = stroke)
                drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.68f, h * 0.34f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.55f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.46f, h * 0.55f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = stroke
                )
            }

            else -> {
                // Study seat
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
        }
    }
}

@Composable
private fun CalendarGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)

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
}

// ---------------------------------------------------------------------------
// TAB CLASSIFICATION
// ---------------------------------------------------------------------------

/**
 * Display-only grouping of reservations that the ViewModel already loaded.
 * - Cancelled: status is CANCELLED / CANCELED
 * - Completed: status is COMPLETED, or the booking's end time has already passed
 * - Upcoming: everything else
 */
private fun bucketOf(
    reservation: MyReservation,
    nowMillis: Long,
    today: String
): BookingTab {
    val status = reservation.status.uppercase()

    if (status == "CANCELLED" || status == "CANCELED") return BookingTab.CANCELLED
    if (status == "COMPLETED") return BookingTab.COMPLETED

    val endMillis = parseIsoMillis(reservation.endAt)

    return if (endMillis != null) {
        if (endMillis < nowMillis) BookingTab.COMPLETED else BookingTab.UPCOMING
    } else {
        // No usable end time: fall back to comparing the booking date with today.
        if (reservation.date.isNotBlank() && reservation.date < today) {
            BookingTab.COMPLETED
        } else {
            BookingTab.UPCOMING
        }
    }
}

private fun parseIsoMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    for (pattern in patterns) {
        try {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = format.parse(value)
            if (parsed != null) return parsed.time
        } catch (e: Exception) {
            // try next pattern
        }
    }

    return null
}

// ---------------------------------------------------------------------------
// FORMATTING
// ---------------------------------------------------------------------------

private fun formatDuration(minutesText: String): String {
    val minutes = minutesText.toIntOrNull() ?: return "$minutesText min"

    return when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

private fun formatDate(value: String): String {
    return try {
        val inputFormat = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        val outputFormat = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

        val date = inputFormat.parse(value)
        date?.let { outputFormat.format(it) } ?: value

    } catch (e: Exception) {
        value
    }
}

private fun formatTime(value: String?): String {
    if (value.isNullOrBlank()) return "--"

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    for (pattern in patterns) {
        try {
            val inputFormat = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val date = inputFormat.parse(value)

            if (date != null) {
                val outputFormat = SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                )
                return outputFormat.format(date)
            }
        } catch (e: Exception) {
            // Try the next timestamp format
        }
    }

    return value
}