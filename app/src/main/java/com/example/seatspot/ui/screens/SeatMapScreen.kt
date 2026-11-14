package com.example.seatspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seatspot.data.model.Seat
import com.example.seatspot.data.model.ComputerStation
import com.example.seatspot.data.repository.FirestoreRepository
import androidx.activity.compose.BackHandler
import com.example.seatspot.ui.components.SeatSpotBottomBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth

private val PrimaryBlue = Color(0xFF2447A8)
private val BackgroundColor = Color(0xFFF3F6FC)
private val CardColor = Color.White

private val AvailableColor = Color(0xFFE0F5E8)
private val ReservedColor = Color(0xFFFFE2E2)
private val InactiveColor = Color(0xFFE5E7EB)
private val SelectedColor = Color(0xFFDCE6FF)

private data class FloorOption(
    val id: String,
    val title: String,
    val description: String,
    val resourceCount: Int,
    val resourceType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatMapScreen(
    onBackClick: () -> Unit,
    onReserveClick: (
        String, Int, String, String, String,
        String, String, String, String
    ) -> Unit,
    onTabClick: (String) -> Unit
) {
    // Reservation date and time
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var hasSelectedStartTime by remember { mutableStateOf(false) }

    var endHour by remember { mutableStateOf(11) }
    var endMinute by remember { mutableStateOf(0) }
    var hasSelectedEndTime by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var choosingStartTime by remember { mutableStateOf(true) }

    var selectedFloor by remember { mutableStateOf<String?>(null) }
    var selectedArea by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var selectedSeat by remember { mutableStateOf<Seat?>(null) }
    var selectedComputer by remember {
        mutableStateOf<ComputerStation?>(null)
    }
    var selectedTable by remember { mutableStateOf<String?>(null) }

    var areas by remember { mutableStateOf<List<String>>(emptyList()) }
    var sections by remember { mutableStateOf<List<String>>(emptyList()) }
    var seats by remember { mutableStateOf<List<Seat>>(emptyList()) }
    var reservedSeats by remember {
        mutableStateOf<Map<String, List<Pair<Long, Long>>>>(emptyMap())
    }
    var computers by remember {
        mutableStateOf<List<ComputerStation>>(emptyList())
    }
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }

    var showAreas by remember { mutableStateOf(false) }
    var showSections by remember { mutableStateOf(false) }
    var showSeats by remember { mutableStateOf(false) }
    var showComputers by remember { mutableStateOf(false) }
    var showTables by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isAvailabilityLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { FirestoreRepository() }

    // Clear all floor/area/seat selections whenever the date or time changes.
    fun resetLocationSelection() {
        selectedFloor = null
        selectedArea = null
        selectedSection = null
        selectedSeat = null
        selectedComputer = null
        selectedTable = null

        areas = emptyList()
        sections = emptyList()
        seats = emptyList()
        computers = emptyList()
        tables = emptyList()
        reservedSeats = emptyMap()

        showAreas = false
        showSections = false
        showSeats = false
        showComputers = false
        showTables = false

        isLoading = false
        isAvailabilityLoading = false
        errorMessage = null
    }

    fun loadSeatAvailability() {

//****************
        android.util.Log.d(
            "SeatSpotAvailability",
            "loadSeatAvailability() was called"
        )

        val user = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser

        val dateMillis = selectedDateMillis ?: return
        val areaId = selectedArea ?: return
        val sectionId = selectedSection ?: return

        isAvailabilityLoading = true
        errorMessage = null

        if (user == null) {
            isAvailabilityLoading = false
            errorMessage = "Please sign in to check availability."
            return
        }

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: run {
                isAvailabilityLoading = false
                errorMessage = "Authentication token unavailable."
                return@addOnSuccessListener
            }

//            *********************
            android.util.Log.d(
                "SeatSpotAvailability",
                "Firebase token obtained. Calling availability API."
            )

            com.example.seatspot.network.SeatSpotApi.getSeatAvailability(
                idToken = token,
                date = reservationDate,
                floorId = "ground",
                areaId = areaId,
                sectionId = sectionId
            ) { result ->

                result.onSuccess { response ->

//                   *********************************
                    android.util.Log.d("SeatSpotAvailability", "API Response: $response")

                    try {
                        val json = org.json.JSONObject(response)
                        val reservations = json.getJSONArray("reservedSeats")

                        val availability = mutableMapOf<
                                String,
                                MutableList<Pair<Long, Long>>
                                >()

                        for (i in 0 until reservations.length()) {
                            val booking = reservations.getJSONObject(i)

                            val resourceId = booking.getString("resourceId")

                            val timestampFormatter = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.US
                            ).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }

                            val startAt = timestampFormatter.parse(
                                booking.getString("startAt")
                            )?.time ?: continue

                            val endAt = timestampFormatter.parse(
                                booking.getString("endAt")
                            )?.time ?: continue

                            availability.getOrPut(resourceId) {
                                mutableListOf()
                            }.add(Pair(startAt, endAt))
                        }

                        reservedSeats = availability
                        isAvailabilityLoading = false

                    } catch (e: Exception) {
                        isAvailabilityLoading = false
                        errorMessage = "Unable to process seat availability."
                    }
                }

                result.onFailure { exception ->

//                   *************************8
                    android.util.Log.e(
                        "SeatSpotAvailability",
                        "API Error",
                        exception
                    )
                    isAvailabilityLoading = false
                    errorMessage = exception.localizedMessage
                        ?: "Unable to load seat availability."
                }
            }
        }?.addOnFailureListener {
            isAvailabilityLoading = false
            errorMessage = "Unable to authenticate availability request."
        }
    }

    fun loadComputerAvailability() {
        val user = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser

        val dateMillis = selectedDateMillis ?: return
        val labId = selectedArea ?: return
        isAvailabilityLoading = true
        errorMessage = null

        if (user == null) {
            isAvailabilityLoading = false
            errorMessage = "Please sign in to check availability."
            return
        }

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: run {
                isAvailabilityLoading = false
                errorMessage = "Authentication token unavailable."
                return@addOnSuccessListener
            }

            com.example.seatspot.network.SeatSpotApi.getSeatAvailability(
                idToken = token,
                date = reservationDate,
                floorId = "first",
                labId = labId
            ) { result ->

                result.onSuccess { response ->
                    try {
                        val json = org.json.JSONObject(response)
                        val reservations = json.getJSONArray("reservedSeats")

                        val availability = mutableMapOf<
                                String,
                                MutableList<Pair<Long, Long>>
                                >()

                        for (i in 0 until reservations.length()) {
                            val booking = reservations.getJSONObject(i)

                            val resourceId = booking.getString("resourceId")

                            val timestampFormatter = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.US
                            ).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }

                            val startAt = timestampFormatter.parse(
                                booking.getString("startAt")
                            )?.time ?: continue

                            val endAt = timestampFormatter.parse(
                                booking.getString("endAt")
                            )?.time ?: continue

                            availability.getOrPut(resourceId) {
                                mutableListOf()
                            }.add(Pair(startAt, endAt))
                        }

                        reservedSeats = availability
                        isAvailabilityLoading = false

                    } catch (e: Exception) {
                        isAvailabilityLoading = false
                        errorMessage = "Unable to process computer availability."
                    }
                }

                result.onFailure { exception ->
                    isAvailabilityLoading = false
                    android.util.Log.e(
                        "SeatSpotAvailability",
                        "Computer availability API error",
                        exception
                    )

                    errorMessage = exception.localizedMessage
                        ?: "Unable to load computer availability."
                }
            }
        }?.addOnFailureListener {
            isAvailabilityLoading = false
            errorMessage = "Unable to authenticate computer availability request."
        }
    }


    fun loadSecondFloorAvailability() {
        val user = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser

        val dateMillis = selectedDateMillis ?: return
        val sectionId = selectedArea ?: return
        val tableId = selectedTable ?: return
        isAvailabilityLoading = true
        errorMessage = null

        if (user == null) {
            isAvailabilityLoading = false
            errorMessage = "Please sign in to check availability."
            return
        }

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: run {
                isAvailabilityLoading = false
                errorMessage = "Authentication token unavailable."
                return@addOnSuccessListener
            }

            com.example.seatspot.network.SeatSpotApi.getSeatAvailability(
                idToken = token,
                date = reservationDate,
                floorId = "second",
                sectionId = sectionId,
                tableId = tableId
            ) { result ->

                result.onSuccess { response ->
                    try {
                        val json = org.json.JSONObject(response)
                        val reservations = json.getJSONArray("reservedSeats")

                        val availability = mutableMapOf<
                                String,
                                MutableList<Pair<Long, Long>>
                                >()

                        for (i in 0 until reservations.length()) {
                            val booking = reservations.getJSONObject(i)

                            val resourceId = booking.getString("resourceId")

                            val timestampFormatter = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.US
                            ).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }

                            val startAt = timestampFormatter.parse(
                                booking.getString("startAt")
                            )?.time ?: continue

                            val endAt = timestampFormatter.parse(
                                booking.getString("endAt")
                            )?.time ?: continue

                            availability.getOrPut(resourceId) {
                                mutableListOf()
                            }.add(Pair(startAt, endAt))
                        }

                        reservedSeats = availability
                        isAvailabilityLoading = false
                    } catch (e: Exception) {
                        isAvailabilityLoading = false
                        errorMessage = "Unable to process Second Floor availability."
                    }
                }

                result.onFailure { exception ->
                    isAvailabilityLoading = false

                    android.util.Log.e(
                        "SeatSpotAvailability",
                        "Second Floor availability API error",
                        exception
                    )

                    errorMessage = exception.localizedMessage
                        ?: "Unable to load Second Floor availability."
                }
            }
        }?.addOnFailureListener {
            isAvailabilityLoading = false
            errorMessage = "Unable to authenticate Second Floor availability request."
        }
    }


    LaunchedEffect(
        selectedDateMillis,
        selectedArea,
        selectedSection,
        selectedFloor,
        showSeats,
        selectedTable,
        showComputers
    ) {
        when {
            showSeats &&
                    selectedFloor == "ground" &&
                    selectedDateMillis != null &&
                    selectedArea != null &&
                    selectedSection != null -> {

                reservedSeats = emptyMap()
                loadSeatAvailability()
            }

            showComputers &&
                    selectedFloor == "first" &&
                    selectedDateMillis != null &&
                    selectedArea != null -> {

                reservedSeats = emptyMap()
                loadComputerAvailability()
            }

            showSeats &&
                    selectedFloor == "second" &&
                    selectedDateMillis != null &&
                    selectedArea != null &&
                    selectedTable != null -> {

                reservedSeats = emptyMap()
                loadSecondFloorAvailability()
            }

            else -> {
                isAvailabilityLoading = false
                reservedSeats = emptyMap()
            }
        }
    }


    val dateFormatter = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val validTime =
        startHour * 60 + startMinute < endHour * 60 + endMinute

    val isFutureTime = remember(
        selectedDateMillis,
        startHour,
        startMinute
    ) {
        val selectedDate = selectedDateMillis

        if (selectedDate == null) {
            false
        } else {
            val indiaTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

            val selectedCalendar = java.util.Calendar.getInstance(indiaTimeZone).apply {
                timeInMillis = selectedDate
                set(java.util.Calendar.HOUR_OF_DAY, startHour)
                set(java.util.Calendar.MINUTE, startMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            selectedCalendar.timeInMillis > System.currentTimeMillis()
        }
    }

    val validSession =
        selectedDateMillis != null &&
                hasSelectedStartTime &&
                hasSelectedEndTime &&
                validTime &&
                isFutureTime

    val floors = listOf(
        FloorOption(
            id = "ground",
            title = "Ground Floor",
            description = "3 Areas • 15 Sections",
            resourceCount = 300,
            resourceType = "Study seats"
        ),
        FloorOption(
            id = "first",
            title = "First Floor",
            description = "3 Computer Labs",
            resourceCount = 90,
            resourceType = "Computers"
        ),
        FloorOption(
            id = "second",
            title = "Second Floor",
            description = "2 Sections • 4 Tables",
            resourceCount = 80,
            resourceType = "Study seats"
        )
    )

    val screenTitle = when {
        !validSession -> "Plan Your Study Session"
        showSeats -> "Choose Your Seat"
        showComputers -> "Choose Your Computer"
        showTables -> "Select a Table"
        showSections -> "Select a Section"
        showAreas && selectedFloor == "first" -> "Select a Computer Lab"
        showAreas && selectedFloor == "second" -> "Select a Section"
        showAreas -> "Select an Area"
        else -> "Select a Floor"
    }


    fun retryCurrentStep() {
        errorMessage = null
        isLoading = true

        when {
            // Retry loading seats on Second Floor
            showTables && selectedTable != null &&
                    selectedFloor == "second" -> {
                repository.getSecondFloorSeats(
                    sectionId = selectedArea!!,
                    tableId = selectedTable!!
                ) { result ->
                    result.onSuccess {
                        seats = it
                        showSeats = true
                        selectedSeat = null
                    }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load seats."
                    }
                    isLoading = false
                }
            }

            // Retry loading tables
            showAreas && selectedArea != null &&
                    selectedFloor == "second" && !showTables -> {
                repository.getSecondFloorTables(selectedArea!!) { result ->
                    result.onSuccess { tables = it }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load tables."
                    }
                    isLoading = false
                }
            }

            // Retry loading Ground Floor seats
            showSections && selectedSection != null &&
                    selectedFloor == "ground" -> {
                repository.getGroundFloorSeats(
                    areaId = selectedArea!!,
                    sectionId = selectedSection!!
                ) { result ->
                    result.onSuccess {
                        seats = it
                        showSeats = true
                        selectedSeat = null
                    }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load seats."
                    }
                    isLoading = false
                }
            }

            // Retry loading Ground Floor sections
            showAreas && selectedArea != null &&
                    selectedFloor == "ground" && !showSections -> {
                repository.getGroundFloorSections(selectedArea!!) { result ->
                    result.onSuccess { sections = it }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load sections."
                    }
                    isLoading = false
                }
            }

            // Retry loading First Floor computers
            showAreas && selectedArea != null &&
                    selectedFloor == "first" && !showComputers -> {
                repository.getFirstFloorComputers(selectedArea!!) { result ->
                    result.onSuccess {
                        computers = it
                        showComputers = true
                        selectedComputer = null
                    }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load computers."
                    }
                    isLoading = false
                }
            }

            // Retry loading areas
            selectedFloor == "ground" && !showAreas -> {
                repository.getGroundFloorAreas { result ->
                    result.onSuccess { areas = it }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load areas."
                    }
                    isLoading = false
                }
            }

            // Retry loading labs
            selectedFloor == "first" && !showAreas -> {
                repository.getFirstFloorLabs { result ->
                    result.onSuccess { areas = it }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load labs."
                    }
                    isLoading = false
                }
            }

            // Retry loading Second Floor sections
            selectedFloor == "second" && !showAreas -> {
                repository.getSecondFloorSections { result ->
                    result.onSuccess { areas = it }
                    result.onFailure {
                        errorMessage = it.localizedMessage
                            ?: "Unable to load sections."
                    }
                    isLoading = false
                }
            }

            else -> {
                isLoading = false
                errorMessage = "Unable to retry this operation."
            }
        }
    }


    fun goBack() {
        when {
            showSeats -> {
                showSeats = false
                selectedSeat = null
            }

            showTables -> {
                showTables = false
                selectedTable = null
            }

            showComputers -> {
                showComputers = false
                selectedComputer = null
            }

            showSections -> {
                showSections = false
                selectedSection = null
            }

            showAreas -> {
                showAreas = false
                selectedArea = null
            }

            else -> onBackClick()
        }
    }
    BackHandler(
        enabled = showSeats || showComputers || showTables || showSections || showAreas
    ) {
        goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        SeatMapHeader(
            onBackClick = { goBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {

            // RESERVATION DATE AND TIME SELECTION

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // CARD TITLE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDDF7F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Study session calendar",
                                tint = Color(0xFF159F91),
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        Text(
                            text = "Select Your Study Session",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172B67)
                        )
                    }

                    // DATE SELECTION
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePicker = true
                            },
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F5FC)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(43.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF20B6A6))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Reservation date",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = selectedDateMillis?.let {
                                        dateFormatter.format(Date(it))
                                    } ?: "Choose your study date",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            }

                            Text(
                                text = "▦",
                                fontSize = 24.sp,
                                color = PrimaryBlue
                            )
                        }
                    }

                    // START AND END TIME
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // START TIME
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = selectedDateMillis != null) {
                                    choosingStartTime = true
                                    showTimePicker = true
                                },
                            shape = RoundedCornerShape(17.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF1F5FC)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {
                                Text(
                                    text = "Start time",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = if (hasSelectedStartTime) {
                                        String.format(
                                            Locale.ROOT,
                                            "%02d:%02d",
                                            startHour,
                                            startMinute
                                        )
                                    } else {
                                        "Choose time"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasSelectedStartTime)
                                        Color(0xFF172B67) else Color.Gray
                                )
                            }
                        }

                        // END TIME
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    enabled = selectedDateMillis != null && hasSelectedStartTime
                                ) {
                                    choosingStartTime = false
                                    showTimePicker = true
                                },
                            shape = RoundedCornerShape(17.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF1F5FC)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {
                                Text(
                                    text = "End time",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = if (hasSelectedEndTime) {
                                        String.format(
                                            Locale.ROOT,
                                            "%02d:%02d",
                                            endHour,
                                            endMinute
                                        )
                                    } else {
                                        "Choose time"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasSelectedEndTime)
                                        Color(0xFF172B67) else Color.Gray
                                )
                            }
                        }
                    }

                    // SESSION VALIDATION MESSAGE

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    validSession -> Color(0xFFE3F7EF)
                                    selectedDateMillis == null -> Color(0xFFEAF1FF)
                                    else -> Color(0xFFFFF4E5)
                                }
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (validSession) "✓" else "ⓘ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                validSession -> Color(0xFF00876C)
                                selectedDateMillis == null -> PrimaryBlue
                                else -> Color(0xFFB7791F)
                            }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = when {
                                selectedDateMillis == null ->
                                    "Step 1 of 3: Select your reservation date."

                                !hasSelectedStartTime ->
                                    "Step 2 of 3: Choose your start time."

                                !hasSelectedEndTime ->
                                    "Now choose your end time to complete your study session."

                                !validTime ->
                                    "End time must be later than start time."

                                !isFutureTime ->
                                    "Please select a future start time."

                                else ->
                                    "Study session selected. You can now choose a floor."
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = when {
                                validSession -> Color(0xFF087F65)
                                selectedDateMillis == null -> Color(0xFF2447A8)
                                else -> Color(0xFF946200)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = screenTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            when {
                showSeats && selectedFloor == "second" -> {
                    Text(
                        text = "Second Floor • ${
                            selectedArea?.replace(
                                "_",
                                " "
                            )
                        } • ${selectedTable?.replace("_", " ")}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

                showSeats -> {
                    Text(
                        text = "Ground Floor • ${selectedArea?.uppercase()} • Section $selectedSection",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

                showComputers -> {
                    Text(
                        text = "First Floor • ${selectedArea?.uppercase()}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(
                    if (showSeats || showComputers) 4 else 1
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                when {
                    !validSession -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "Complete your study session first",
                                        color = Color(0xFF172B67),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Select a date, then choose your start and end times. Floor options will appear after your session is valid.",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    isAvailabilityLoading && (showSeats || showComputers) -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryBlue
                                )

                                Text(
                                    text = "Checking availability...",
                                    color = PrimaryBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )

                            }
                        }
                    }
                    // EMPTY STATE: NO SEATS
                    showSeats && seats.isEmpty() -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AvailabilityEmptyState(
                                title = "No seats found",
                                message = "There are currently no seats listed in this section. Please try another section."
                            )
                        }
                    }

// EMPTY STATE: NO COMPUTERS
                    showComputers && computers.isEmpty() -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AvailabilityEmptyState(
                                title = "No computers found",
                                message = "There are currently no computers listed in this lab. Please try another lab."
                            )
                        }
                    }

                    // GROUND FLOOR SEATS
                    showSeats -> {
                        items(
                            items = seats,
                            key = { it.seatId }
                        ) { seat ->
                            SeatCard(
                                seat = seat,
                                isSelected = selectedSeat?.seatId == seat.seatId,
                                isReserved = reservedSeats[seat.seatId]?.any { (start, end) ->

                                    val selectedStart = selectedDateMillis?.let { dateMillis ->
                                        java.util.Calendar.getInstance(
                                            TimeZone.getTimeZone("Asia/Kolkata")
                                        ).apply {
                                            timeInMillis = dateMillis
                                            set(java.util.Calendar.HOUR_OF_DAY, startHour)
                                            set(java.util.Calendar.MINUTE, startMinute)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    } ?: 0L

                                    val selectedEnd = selectedDateMillis?.let { dateMillis ->
                                        java.util.Calendar.getInstance(
                                            TimeZone.getTimeZone("Asia/Kolkata")
                                        ).apply {
                                            timeInMillis = dateMillis
                                            set(java.util.Calendar.HOUR_OF_DAY, endHour)
                                            set(java.util.Calendar.MINUTE, endMinute)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    } ?: 0L

                                    selectedStart < end && selectedEnd > start

                                } == true,
                                onClick = {
                                    selectedSeat = seat
                                }
                            )
                        }
                    }

                    // FIRST FLOOR COMPUTERS
                    showComputers -> {
                        items(
                            items = computers,
                            key = { it.computerId }
                        ) { computer ->

                            val selectedStart = selectedDateMillis?.let { dateMillis ->
                                java.util.Calendar.getInstance(
                                    TimeZone.getTimeZone("Asia/Kolkata")
                                ).apply {
                                    timeInMillis = dateMillis
                                    set(java.util.Calendar.HOUR_OF_DAY, startHour)
                                    set(java.util.Calendar.MINUTE, startMinute)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            } ?: 0L

                            val selectedEnd = selectedDateMillis?.let { dateMillis ->
                                java.util.Calendar.getInstance(
                                    TimeZone.getTimeZone("Asia/Kolkata")
                                ).apply {
                                    timeInMillis = dateMillis
                                    set(java.util.Calendar.HOUR_OF_DAY, endHour)
                                    set(java.util.Calendar.MINUTE, endMinute)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            } ?: 0L

                            val isReserved =
                                reservedSeats[computer.computerId]?.any { (start, end) ->
                                    selectedStart < end && selectedEnd > start
                                } == true

                            ComputerCard(
                                computer = computer,
                                isSelected = selectedComputer?.computerId ==
                                        computer.computerId,
                                isReserved = isReserved,
                                onClick = {
                                    selectedComputer = computer
                                }
                            )
                        }
                    }


                    // SECOND FLOOR TABLES
                    showTables -> {
                        items(tables) { table ->
                            SelectionCard(
                                title = table.replace("_", " ")
                                    .replaceFirstChar { it.uppercase() },
                                subtitle = "20 Seats",
                                isSelected = selectedTable == table,
                                onClick = {
                                    selectedTable = table
                                }
                            )
                        }
                    }

                    // GROUND FLOOR SECTIONS
                    showSections -> {
                        items(sections) { section ->
                            SelectionCard(
                                title = "Section $section",
                                subtitle = "20 Seats",
                                isSelected = selectedSection == section,
                                onClick = {
                                    selectedSection = section
                                }
                            )
                        }
                    }

                    // GROUND FLOOR AREAS / FIRST FLOOR LABS
                    showAreas -> {
                        items(areas) { area ->
                            SelectionCard(
                                title = area.replace("_", " ")
                                    .split(" ")
                                    .mapIndexed { index, part ->
                                        if (index == 0) {
                                            part.replaceFirstChar { it.uppercase() }
                                        } else {
                                            part.uppercase()
                                        }
                                    }
                                    .joinToString(" "),
                                subtitle = when (selectedFloor) {
                                    "first" -> "30 Computers"
                                    "second" -> "2 Tables"
                                    else -> "5 Sections"
                                },
                                isSelected = selectedArea == area,
                                onClick = {
                                    selectedArea = area
                                }
                            )
                        }
                    }

                    // FLOOR SELECTION
                    else -> {
                        items(floors) { floor ->
                            FloorCard(
                                floor = floor,
                                isSelected = selectedFloor == floor.id,
                                onClick = {
                                    resetLocationSelection()
                                    selectedFloor = floor.id
                                }
                            )
                        }
                    }
                }
            }


            // GROUND FLOOR SEAT SELECTION
            if (showSeats || showComputers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(AvailableColor, "Available")
                    LegendItem(ReservedColor, "Reserved")
                    LegendItem(InactiveColor, "Inactive")
                    LegendItem(SelectedColor, "Selected")
                }
                if (showSeats) {
                    Text(
                        text = selectedSeat?.let {
                            "Selected seat: ${it.seatId}"
                        } ?: "Select an available seat.",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Button(
                    onClick = {
                        selectedSeat?.let { seat ->
                            val dateMillis = selectedDateMillis

                            if (dateMillis != null) {
                                val reservationDate = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
                                ).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }.format(Date(dateMillis))

                                val startTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    startHour,
                                    startMinute
                                )

                                val endTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    endHour,
                                    endMinute
                                )

                                if (selectedFloor == "second") {
                                    val sectionId = selectedArea
                                    val tableId = selectedTable

                                    if (sectionId != null && tableId != null) {
                                        onReserveClick(
                                            seat.seatId,
                                            seat.seatNumber,
                                            "",
                                            sectionId,
                                            tableId,
                                            reservationDate,
                                            startTime,
                                            endTime,
                                            "SECOND_FLOOR_SEAT"
                                        )
                                    }
                                } else {
                                    val areaId = selectedArea
                                    val sectionId = selectedSection

                                    if (areaId != null && sectionId != null) {
                                        onReserveClick(
                                            seat.seatId,
                                            seat.seatNumber,
                                            areaId,
                                            sectionId,
                                            "",
                                            reservationDate,
                                            startTime,
                                            endTime,
                                            "SEAT"
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedSeat != null && validSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF172B67),
                        disabledContainerColor = Color(0xFFB8C1D6)
                    )
                ) {
                    Text(
                        text = "Continue to Reserve  →",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FIRST FLOOR COMPUTER SELECTION

            else if (showComputers) {
                Text(
                    text = selectedComputer?.let {
                        "Selected computer: PC ${it.computerNumber}"
                    } ?: "Select an available computer.",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Button(
                    onClick = {
                        selectedComputer?.let { computer ->

                            val labId = selectedArea
                            val dateMillis = selectedDateMillis

                            if (labId != null && dateMillis != null) {

                                val reservationDate = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
                                ).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }.format(Date(dateMillis))

                                val startTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    startHour,
                                    startMinute
                                )

                                val endTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    endHour,
                                    endMinute
                                )

                                onReserveClick(
                                    computer.computerId,
                                    computer.computerNumber,
                                    labId,
                                    "",
                                    "",
                                    reservationDate,
                                    startTime,
                                    endTime,
                                    "COMPUTER"
                                )
                            }
                        }
                    },
                    enabled = selectedComputer != null && validSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF172B67),
                        disabledContainerColor = Color(0xFFB8C1D6)
                    )
                ) {
                    Text(
                        text = "Continue to Reserve  →",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            // FLOOR / AREA / SECTION / LAB CONTINUE BUTTON
            else if (validSession) {
                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true

                        when {

                            // GROUND FLOOR: LOAD AREAS
                            !showAreas && selectedFloor == "ground" -> {
                                repository.getGroundFloorAreas { result ->
                                    result.onSuccess { fetchedAreas ->
                                        areas = fetchedAreas
                                        showAreas = true
                                        selectedArea = null
                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load areas."
                                    }

                                    isLoading = false
                                }
                            }

                            // FIRST FLOOR: LOAD LABS
                            !showAreas && selectedFloor == "first" -> {
                                repository.getFirstFloorLabs { result ->
                                    result.onSuccess { fetchedLabs ->
                                        areas = fetchedLabs
                                        showAreas = true
                                        selectedArea = null
                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load computer labs."
                                    }

                                    isLoading = false
                                }
                            }

                            // GROUND FLOOR: LOAD SECTIONS
                            showAreas &&
                                    selectedFloor == "ground" &&
                                    !showSections -> {
                                val areaId = selectedArea!!

                                repository.getGroundFloorSections(areaId) { result ->
                                    result.onSuccess { fetchedSections ->
                                        sections = fetchedSections
                                        showSections = true
                                        selectedSection = null

                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load sections."
                                    }

                                    isLoading = false
                                }
                            }

                            // FIRST FLOOR: LOAD COMPUTERS
                            showAreas && selectedFloor == "first" -> {
                                val labId = selectedArea!!

                                repository.getFirstFloorComputers(labId) { result ->
                                    result.onSuccess { fetchedComputers ->
                                        computers = fetchedComputers
                                        showComputers = true
                                        selectedComputer = null


                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load computers."
                                    }

                                    isLoading = false
                                }
                            }

                            // SECOND FLOOR: LOAD SECTIONS
                            !showAreas && selectedFloor == "second" -> {
                                repository.getSecondFloorSections { result ->
                                    result.onSuccess { fetchedSections ->
                                        areas = fetchedSections
                                        showAreas = true
                                        selectedArea = null
                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load Second Floor sections."
                                    }

                                    isLoading = false
                                }
                            }


                            // SECOND FLOOR: LOAD TABLES
                            showAreas &&
                                    selectedFloor == "second" &&
                                    !showTables -> {
                                val sectionId = selectedArea!!

                                repository.getSecondFloorTables(sectionId) { result ->
                                    result.onSuccess { fetchedTables ->
                                        tables = fetchedTables
                                        showTables = true
                                        selectedTable = null

                                        if (fetchedTables.isEmpty()) {
                                            errorMessage = "No tables found in this section."
                                        }
                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load Second Floor tables."
                                    }

                                    isLoading = false
                                }
                            }

                            // SECOND FLOOR: LOAD SEATS
                            showTables &&
                                    selectedFloor == "second" &&
                                    selectedTable != null -> {
                                val sectionId = selectedArea!!
                                val tableId = selectedTable!!

                                repository.getSecondFloorSeats(
                                    sectionId = sectionId,
                                    tableId = tableId
                                ) { result ->
                                    result.onSuccess { fetchedSeats ->
                                        seats = fetchedSeats
                                        showSeats = true
                                        selectedSeat = null

                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load Second Floor seats."
                                    }

                                    isLoading = false
                                }
                            }

                            // GROUND FLOOR: LOAD SEATS
                            showSections &&
                                    selectedFloor == "ground" &&
                                    selectedSection != null -> {
                                val areaId = selectedArea!!
                                val sectionId = selectedSection!!

                                repository.getGroundFloorSeats(
                                    areaId = areaId,
                                    sectionId = sectionId
                                ) { result ->
                                    result.onSuccess { fetchedSeats ->
                                        seats = fetchedSeats
                                        showSeats = true
                                        selectedSeat = null
                                    }

                                    result.onFailure { exception ->
                                        errorMessage = exception.localizedMessage
                                            ?: "Unable to load seats."
                                    }
                                    isLoading = false
                                }
                            }

                            else -> {
                                isLoading = false
                                errorMessage =
                                    "This section is not available yet."
                            }
                        }
                    },
                    enabled = !isLoading && (
                            (!showAreas &&
                                    selectedFloor in listOf("ground", "first", "second")) ||
                                    (showAreas &&
                                            !showSections &&
                                            selectedArea != null &&
                                            (selectedFloor != "second" || !showTables)) ||
                                    (showSections &&
                                            selectedFloor == "ground" &&
                                            selectedSection != null) ||
                                    (showTables &&
                                            selectedFloor == "second" &&
                                            selectedTable != null)
                            ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFDCE6F7),
                        disabledContentColor = Color(0xFF8492AD)
                    )
                ) {
                    Text(if (isLoading) "Loading..." else "Continue")
                }
            }


            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEEEE)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Something went wrong",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB42318)
                        )

                        Text(
                            text = message,
                            fontSize = 13.sp,
                            color = Color(0xFF7F1D1D)
                        )

                        Button(
                            onClick = {
                                if (showSeats || showComputers) {
                                    when {
                                        showComputers -> loadComputerAvailability()
                                        selectedFloor == "second" ->
                                            loadSecondFloorAvailability()

                                        showSeats -> loadSeatAvailability()
                                    }
                                } else {
                                    retryCurrentStep()
                                }
                            },
                            enabled = !isAvailabilityLoading && !isLoading
                        ) {
                            Text(
                                if (isAvailabilityLoading || isLoading) "Retrying..."
                                else "Try Again"
                            )
                        }
                    }
                }
            }


            TextButton(
                onClick = { goBack() },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text(
                    text = when {
                        showSeats -> "Back to Sections"
                        showComputers -> "Back to Labs"
                        showTables -> "Back to Sections"
                        showSections -> "Back to Areas"
                        showAreas -> "Back to Floors"
                        else -> "Back to Home"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        SeatSpotBottomBar(
            selectedRoute = "seatmap",
            onDestinationClick = { route ->
                onTabClick(route)
            }
        )

        // DATE PICKER
        if (showDatePicker) {
            val todayUtcMillis = remember {
                java.util.Calendar.getInstance(
                    TimeZone.getTimeZone("UTC")
                ).apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis,
                selectableDates = object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= todayUtcMillis
                    }
                }
            )

            DatePickerDialog(
                onDismissRequest = {
                    showDatePicker = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newDate = datePickerState.selectedDateMillis

                            if (newDate != selectedDateMillis) {
                                resetLocationSelection()
                                selectedDateMillis = newDate

                                startHour = 9
                                startMinute = 0
                                hasSelectedStartTime = false

                                endHour = 11
                                endMinute = 0
                                hasSelectedEndTime = false
                            }

                            showDatePicker = false
                        }
                    ) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // TIME PICKER
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = if (choosingStartTime) startHour else endHour,
                initialMinute = if (choosingStartTime) startMinute else endMinute,
                is24Hour = true
            )

            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showTimePicker = false
                },
                title = {
                    Text(
                        if (choosingStartTime) {
                            "Choose start time"
                        } else {
                            "Choose end time"
                        }
                    )
                },
                text = {
                    TimePicker(state = timePickerState)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (choosingStartTime) {
                                resetLocationSelection()
                                startHour = timePickerState.hour
                                startMinute = timePickerState.minute
                                hasSelectedStartTime = true
                                hasSelectedEndTime = false
                            } else {
                                resetLocationSelection()
                                endHour = timePickerState.hour
                                endMinute = timePickerState.minute
                                hasSelectedEndTime = true
                            }

                            showTimePicker = false
                        }
                    ) {
                        Text("Set time")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTimePicker = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Composable
private fun SeatCard(
    seat: Seat,
    isSelected: Boolean,
    isReserved: Boolean,
    onClick: () -> Unit
) {
    val seatColor = when {
        !seat.isActive -> InactiveColor
        isReserved -> ReservedColor
        isSelected -> SelectedColor
        else -> AvailableColor
    }

    val status = when {
        !seat.isActive -> "Inactive"
        isReserved -> "Reserved"
        isSelected -> "Selected"
        else -> "Available"
    }

    val statusColor = when {
        !seat.isActive -> Color(0xFF6B7280)
        isReserved -> Color(0xFFCC4242)
        isSelected -> PrimaryBlue
        else -> Color(0xFF16845D)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable(
                enabled = seat.isActive && !isReserved,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = seatColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> PrimaryBlue
                isReserved -> Color(0xFFF3B5B5)
                !seat.isActive -> Color(0xFFD1D5DB)
                else -> Color(0xFFB9E5C9)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chair,
                contentDescription = "Seat",
                tint = statusColor,
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "S${seat.seatNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF172B67)
            )

            Text(
                text = status,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}


@Composable
private fun ComputerCard(
    computer: ComputerStation,
    isSelected: Boolean,
    isReserved: Boolean,
    onClick: () -> Unit
) {
    val computerColor = when {
        !computer.isActive -> InactiveColor
        isReserved -> ReservedColor
        isSelected -> SelectedColor
        else -> AvailableColor
    }

    val status = when {
        !computer.isActive -> "Inactive"
        isReserved -> "Reserved"
        isSelected -> "Selected"
        else -> "Available"
    }

    val statusColor = when {
        !computer.isActive -> Color(0xFF6B7280)
        isReserved -> Color(0xFFCC4242)
        isSelected -> PrimaryBlue
        else -> Color(0xFF16845D)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable(
                enabled = computer.isActive && !isReserved,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = computerColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> PrimaryBlue
                isReserved -> Color(0xFFF3B5B5)
                !computer.isActive -> Color(0xFFD1D5DB)
                else -> Color(0xFFB9E5C9)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Monitor,
                contentDescription = "Computer",
                tint = statusColor,
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "PC ${computer.computerNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF172B67)
            )

            Text(
                text = status,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}


@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color,
                    RoundedCornerShape(3.dp)
                )
        )

        Text(label, fontSize = 11.sp)
    }
}



@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val teal = Color(0xFF20B6A6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFFE8EEFF)
            else
                Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                PrimaryBlue
            else
                Color(0xFFE5EAF3)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected)
                            Color(0xFFD5E1FF)
                        else
                            Color(0xFFDDF7F2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = if (isSelected) PrimaryBlue else teal,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172B67)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Icon(
                imageVector = if (isSelected)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isSelected) PrimaryBlue else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun FloorCard(
    floor: FloorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val navy = Color(0xFF172B67)
    val teal = Color(0xFF20B6A6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFFE8EEFF)
            else
                Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                PrimaryBlue
            else
                Color(0xFFE5EAF3)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (isSelected)
                            Color(0xFFD5E1FF)
                        else
                            Color(0xFFDDF7F2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (floor.id) {
                        "ground" -> Icons.Default.Chair
                        "first" -> Icons.Default.Monitor
                        "second" -> Icons.Default.Layers
                        else -> Icons.Default.MeetingRoom
                    },
                    contentDescription = floor.title,
                    tint = if (isSelected) PrimaryBlue else teal,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = floor.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = navy
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = floor.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "${floor.resourceCount} ${floor.resourceType}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(25.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select floor",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@Composable
private fun AvailabilityEmptyState(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = null,
                tint = Color(0xFF20B6A6),
                modifier = Modifier.size(42.dp)
            )

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172B67)
            )

            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


@Composable
private fun SeatMapHeader(
    onBackClick: () -> Unit
) {
    val deepIndigo = Color(0xFF1B2A6B)
    val royalBlue = Color(0xFF2447A8)
    val mint = Color(0xFF7FE8D8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    bottomStart = 34.dp,
                    bottomEnd = 34.dp
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(deepIndigo, royalBlue)
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.42f,
                    center = Offset(
                        size.width * 0.98f,
                        size.height * 0.05f
                    )
                )

                drawCircle(
                    color = Color(0xFF1FB5A8).copy(alpha = 0.22f),
                    radius = size.width * 0.30f,
                    center = Offset(
                        size.width * 0.04f,
                        size.height * 1.02f
                    )
                )
            }
            .padding(
                start = 22.dp,
                end = 22.dp,
                top = 38.dp,
                bottom = 32.dp
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { onBackClick() }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
            ) {
                Text(
                    text = "‹  Back",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Find a Seat",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose your preferred study space.",
                color = mint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

