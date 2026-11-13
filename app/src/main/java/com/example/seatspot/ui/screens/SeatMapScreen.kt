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

private val PrimaryBlue = Color(0xFF2447A8)
private val BackgroundColor = Color(0xFFF3F6FC)
private val CardColor = Color.White
private val AvailableColor = Color(0xFFC8F2D5)
private val InactiveColor = Color(0xFFF7C7C7)
private val SelectedColor = Color(0xFFB9C9FF)

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

    var endHour by remember { mutableStateOf(11) }
    var endMinute by remember { mutableStateOf(0) }

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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val repository = remember { FirestoreRepository() }
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

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: return@addOnSuccessListener

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

                    } catch (e: Exception) {
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

                    errorMessage = exception.localizedMessage
                        ?: "Unable to load seat availability."
                }
            }
        }?.addOnFailureListener {
            errorMessage = "Unable to authenticate availability request."
        }
    }

    fun loadComputerAvailability() {
        val user = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser

        val dateMillis = selectedDateMillis ?: return
        val labId = selectedArea ?: return

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: return@addOnSuccessListener

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

                    } catch (e: Exception) {
                        errorMessage = "Unable to process computer availability."
                    }
                }

                result.onFailure { exception ->
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

        val reservationDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))

        user?.getIdToken(false)?.addOnSuccessListener { tokenResult ->

            val token = tokenResult.token ?: return@addOnSuccessListener

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

                    } catch (e: Exception) {
                        errorMessage = "Unable to process Second Floor availability."
                    }
                }

                result.onFailure { exception ->
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
        selectedDateMillis != null && validTime && isFutureTime

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
        showSeats -> "Choose Your Seat"
        showComputers -> "Choose Your Computer"
        showTables -> "Select a Table"
        showSections -> "Select a Section"
        showAreas && selectedFloor == "first" -> "Select a Computer Lab"
        showAreas && selectedFloor == "second" -> "Select a Section"
        showAreas -> "Select an Area"
        else -> "Select a Floor"
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Choose Your Space",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Central Library",
                color = Color.Gray,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(24.dp))


            // RESERVATION DATE AND TIME SELECTION

            Text(
                text = "Select Your Study Session",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(10.dp))

// Date selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RESERVATION DATE",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = selectedDateMillis?.let {
                            dateFormatter.format(Date(it))
                        } ?: "Choose your study date",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Start and end time selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            choosingStartTime = true
                            showTimePicker = true
                        },
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("START TIME", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            String.format(
                                Locale.ROOT,
                                "%02d:%02d",
                                startHour,
                                startMinute
                            ),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            choosingStartTime = false
                            showTimePicker = true
                        },
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("END TIME", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            String.format(
                                Locale.ROOT,
                                "%02d:%02d",
                                endHour,
                                endMinute
                            ),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    selectedDateMillis == null ->
                        "Please select a reservation date."

                    !validTime ->
                        "End time must be later than start time."

                    !isFutureTime ->
                        "Please select a future start time."

                    else ->
                        "Availability will be checked for your selected session."
                },
                color = if (validSession) Color.Gray else Color(0xFFD32F2F)
            )

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
                    if (showSeats || showComputers) 4 else 2
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                when {
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

                            val isReserved = reservedSeats[computer.computerId]?.any {
                                    (start, end) ->
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
                                    .replaceFirstChar { it.uppercase() },
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
                                    selectedFloor = floor.id
                                    errorMessage = null
                                }
                            )
                        }

                        item {
                            FloorCard(
                                floor = FloorOption(
                                    id = "rooms",
                                    title = "Discussion Rooms",
                                    description = "Private study rooms",
                                    resourceCount = 2,
                                    resourceType = "Rooms"
                                ),
                                isSelected = selectedFloor == "rooms",
                                onClick = {
                                    selectedFloor = "rooms"
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }
            }



            // GROUND FLOOR SEAT SELECTION
            if (showSeats) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(AvailableColor, "Active")
                    LegendItem(InactiveColor, "Inactive")
                    LegendItem(SelectedColor, "Selected")
                }

                Text(
                    text = selectedSeat?.let {
                        "Selected seat: ${it.seatId}"
                    } ?: "Select an active seat.",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to Reserve")
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to Reserve")
                }
            }


            // FLOOR / AREA / SECTION / LAB CONTINUE BUTTON
            else {
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

                                        if (fetchedComputers.isEmpty()) {
                                            errorMessage =
                                                "No computers found in this lab."
                                        }
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

                                        if (fetchedSeats.isEmpty()) {
                                            errorMessage = "No seats found at this table."
                                        }
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


                                        if (fetchedSeats.isEmpty()) {
                                            errorMessage = "No seats found in this section."
                                        }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLoading) "Loading..." else "Continue")
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            TextButton(
                onClick = { goBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        showSeats -> "Back to Sections"
                        showComputers -> "Back to Labs"
                        showTables -> "Back to Sections"
                        showSections -> "Back to Areas"
                        showAreas -> "Back to Floors"
                        else -> "Back to Home"
                    }
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
                            selectedDateMillis =
                                datePickerState.selectedDateMillis
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
                                startHour = timePickerState.hour
                                startMinute = timePickerState.minute
                            } else {
                                endHour = timePickerState.hour
                                endMinute = timePickerState.minute
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
        isSelected -> SelectedColor
        isReserved -> InactiveColor
        else -> AvailableColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(
                enabled = seat.isActive && !isReserved,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = seatColor
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "S${seat.seatNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = when {
                    !seat.isActive -> "Inactive"
                    isReserved -> "Reserved"
                    isSelected -> "Selected"
                    else -> "Available"
                },
                fontSize = 10.sp
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
        isReserved -> InactiveColor
        isSelected -> SelectedColor
        else -> AvailableColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable(
                enabled = computer.isActive && !isReserved,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = computerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Monitor,
                contentDescription = "Computer",
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = "PC ${computer.computerNumber}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Text(
                text = when {
                    !computer.isActive -> "Inactive"
                    isReserved -> "Reserved"
                    isSelected -> "Selected"
                    else -> "Available"
                },
                fontSize = 9.sp
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFE0E9FF)
            } else {
                CardColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(185.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFE0E9FF)
            } else {
                CardColor
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = when (floor.id) {
                    "ground" -> Icons.Default.Chair
                    "first" -> Icons.Default.Monitor
                    "second" -> Icons.Default.Layers
                    else -> Icons.Default.MeetingRoom
                },
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(30.dp)
            )

            Column {
                Text(
                    text = floor.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF172554)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = floor.description,
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${floor.resourceCount} ${floor.resourceType}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

