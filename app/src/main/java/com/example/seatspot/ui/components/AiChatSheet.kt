package com.example.seatspot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seatspot.data.model.AiOption
import com.example.seatspot.data.model.AiProposal
import com.example.seatspot.data.model.ChatMessage
import com.example.seatspot.viewmodel.AiBookingUiState
import com.example.seatspot.viewmodel.AiBookingViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind

private val DeepIndigo = Color(0xFF1B2A6B)
private val RoyalBlue = Color(0xFF2447A8)
private val PrimaryText = Color(0xFF172554)
private val SecondaryText = Color(0xFF64748B)
private val SoftBlue = Color(0xFFE8EEFF)
private val SoftIndigo = Color(0xFFE4E7F6)
private val ScreenBackground = Color(0xFFF3F6FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (AiOption, AiProposal) -> Unit,
    viewModel: AiBookingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBackground
    ) {
        AiChatSheetContent(
            uiState = uiState,
            onInputChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.send() },
            onClear = { viewModel.clearChat() },
            onDismiss = onDismiss,
            onOptionSelected = onOptionSelected
        )
    }
}
@Composable
fun AiChatSheetContent(
    uiState: AiBookingUiState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onOptionSelected: (AiOption, AiProposal) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val exampleChips = listOf(
        "Tomorrow 4 to 6 pm, any available seat",
        "Find an available computer for 2 hours",
        "Sunday 10 am to 1 pm, second floor",
        "I need a discussion room for 4 people"
    )

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFF),
                        Color(0xFFEDF3FF),
                        Color(0xFFF3F6FC)
                    )
                )
            )
            .drawBehind {

                // Soft blue glow in the upper-right corner
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB8CCFF).copy(alpha = 0.32f),
                            Color.Transparent
                        ),
                        center = Offset(
                            size.width * 0.95f,
                            size.height * 0.12f
                        ),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(
                        size.width * 0.95f,
                        size.height * 0.12f
                    )
                )

                // Subtle teal glow near the bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB8F0E9).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(
                            size.width * 0.05f,
                            size.height * 0.88f
                        ),
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = Offset(
                        size.width * 0.05f,
                        size.height * 0.88f
                    )
                )
            }
    ) {
        // HEADER

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI icon background
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(SoftBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "SeatSpot AI",
                    tint = RoyalBlue,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SeatSpot AI",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Find your perfect study space",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }

            TextButton(onClick = onClear) {
                Text(
                    text = "Clear",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close AI Assistant",
                    tint = SecondaryText
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SoftIndigo)
        )

        // CHAT MESSAGES LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                vertical = 20.dp
            )
        ) {
            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                ChatMessageItem(
                    message = message,
                    onOptionSelected = onOptionSelected
                )
            }

            // EXAMPLE CHIPS (shown when user hasn't sent messages yet)

            if (uiState.messages.size <= 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "QUICK SUGGESTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryText,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        exampleChips.chunked(2).forEach { chipRow ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                chipRow.forEach { chipText ->

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(68.dp)
                                            .clickable {
                                                onInputChanged(chipText)
                                            },
                                        shape = RoundedCornerShape(13.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, SoftIndigo),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 0.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chipText,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryText,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(
                                                modifier = Modifier.width(5.dp)
                                            )

                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = RoyalBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                if (chipRow.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // FOOTER INPUT AREA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Write the full request each time",
                fontSize = 11.sp,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Ask AI for a seat or computer...",
                            fontSize = 14.sp,
                            color = SecondaryText
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = SoftIndigo,
                        focusedContainerColor = ScreenBackground,
                        unfocusedContainerColor = ScreenBackground
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                val canSend = uiState.inputText.isNotBlank() && !uiState.isLoading

                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSend) RoyalBlue else Color(0xFFCBD5E1))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send request",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI can make mistakes. Check date and time before you confirm.",
                    fontSize = 11.sp,
                    color = SecondaryText,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${uiState.inputText.length}/200",
                    fontSize = 11.sp,
                    color = if (uiState.inputText.length >= 200) Color.Red else SecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onOptionSelected: (AiOption, AiProposal) -> Unit
) {
    if (message.isUser) {
        // USER BUBBLE
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(RoyalBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        // ASSISTANT BUBBLE
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            if (message.isTyping) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftBlue)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = RoyalBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI is checking availability...",
                        fontSize = 13.sp,
                        color = DeepIndigo,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 18.dp
                            )
                        )
                        .background(SoftBlue)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = PrimaryText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // PROPOSAL OPTIONS
                message.proposal?.let { proposal ->
                    if (proposal.options.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SoftIndigo),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ScreenBackground)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Date",
                                            tint = RoyalBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = proposal.date,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepIndigo
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Time",
                                            tint = RoyalBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${proposal.startTime} - ${proposal.endTime}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBlue
                                        )
                                    }
                                }

                                proposal.options.forEach { option ->
                                    OptionCardItem(
                                        option = option,
                                        onSelect = {
                                            onOptionSelected(option, proposal)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCardItem(
    option: AiOption,
    onSelect: () -> Unit
) {
    val displayLabel = option.label.ifBlank {
        when (option.resourceType.uppercase()) {
            "DISCUSSION_ROOM" -> {
                "Discussion Room · ${option.resourceId}"
            }

            "COMPUTER" -> {
                "Computer Lab ${option.labId.ifEmpty { option.areaId }} · PC ${option.seatNumber}"
            }

            "SECOND_FLOOR_SEAT" -> {
                "Second Floor · Section ${option.sectionId} · Table ${option.tableId} · Seat ${option.seatNumber}"
            }

            else -> {
                "Ground Floor · Area ${option.areaId} · Section ${option.sectionId} · Seat ${option.seatNumber}"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (option.resourceType.uppercase() == "DISCUSSION_ROOM") {
                    Text(
                        text = "Capacity: ${option.capacity} people",
                        fontSize = 11.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Select",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiChatSheetPreview() {
    val sampleProposal = AiProposal(
        date = "2026-11-15",
        startTime = "16:00",
        endTime = "18:00",
        options = listOf(
            AiOption(
                resourceType = "COMPUTER",
                resourceId = "pc_101",
                labId = "lab_1",
                seatNumber = 5,
                label = "First Floor · Lab 1 · PC 5"
            ),
            AiOption(
                resourceType = "SEAT",
                resourceId = "seat_202",
                areaId = "area_a",
                sectionId = "sec_1",
                seatNumber = 12,
                label = "Ground Floor · Area A · Section 1 · Seat 12"
            )
        )
    )

    val sampleState = AiBookingUiState(
        messages = listOf(
            ChatMessage(
                isUser = false,
                text = "Hi! I'm your AI Booking Assistant. Tell me what kind of seat or computer you need."
            ),
            ChatMessage(
                isUser = true,
                text = "Tomorrow 4 to 6 pm Mac computer"
            ),
            ChatMessage(
                isUser = false,
                text = "Here are matching options for your requested slot:",
                proposal = sampleProposal
            )
        ),
        isLoading = false,
        inputText = ""
    )

    Box(modifier = Modifier.background(ScreenBackground)) {
        AiChatSheetContent(
            uiState = sampleState,
            onInputChanged = {},
            onSend = {},
            onClear = {},
            onDismiss = {},
            onOptionSelected = { _, _ -> }
        )
    }
}
