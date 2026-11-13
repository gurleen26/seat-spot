
package com.example.seatspot.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.seatspot.R

private val DeepIndigo = Color(0xFF1B2A6B)
private val RoyalBlue = Color(0xFF2447A8)
private val Teal = Color(0xFF1FB5A8)
private val Mint = Color(0xFF7FE8D8)

@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    onSignIn: () -> Unit
) {

    // Entrance animations
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(35f) }
    val taglineAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }

    // Coordinated entrance sequence
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(500))

        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        kotlinx.coroutines.delay(250)

        titleAlpha.animateTo(1f, tween(600))
        titleOffset.animateTo(
            0f,
            tween(600, easing = FastOutSlowInEasing)
        )

        kotlinx.coroutines.delay(250)
        taglineAlpha.animateTo(1f, tween(600))

        kotlinx.coroutines.delay(300)
        bottomAlpha.animateTo(1f, tween(600))
    }

    // Animated logo rings
    val pulse = rememberInfiniteTransition(label = "pulse")

    val ringProgress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepIndigo,
                        RoyalBlue,
                        Teal
                    )
                )
            )
    ) {

        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {

            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.minDimension * 0.55f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.95f,
                    size.height * 0.05f
                )
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.45f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.05f,
                    size.height * 0.95f
                )
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo and animated rings
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.fillMaxSize()) {

                    val maxR = size.minDimension / 2f

                    listOf(0f, 0.5f).forEach { offset ->

                        val p = (ringProgress + offset) % 1f

                        drawCircle(
                            color = Mint.copy(
                                alpha = (1f - p) * 0.30f
                            ),
                            radius = maxR * (0.55f + 0.45f * p),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx()
                            )
                        )
                    }
                }

                // Main logo background
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .shadow(
                            elevation = 22.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE6F7F5)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_seat_logo
                        ),
                        contentDescription = "SeatSpot chair logo",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App name
            Text(
                text = "SeatSpot",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = Color.White,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .graphicsLayer {
                        translationY = titleOffset.value
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline
            Text(
                text = "Find your focus.\nReserve your seat.",
                fontSize = 17.sp,
                lineHeight = 25.sp,
                color = Color.White.copy(alpha = 0.90f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Feature chips
            Row(
                modifier = Modifier.alpha(taglineAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                listOf(
                    "Live seats",
                    "Quick booking",
                    "No queues"
                ).forEach { label ->

                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 24.dp,
                    vertical = 28.dp
                )
                .alpha(bottomAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Get Started button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = RoyalBlue
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 7.dp
                )
            ) {

                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sign in text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Already booked? ",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Text(
                    text = "Sign in",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable(
                        onClick = onSignIn
                    )
                )
            }
        }
    }
}
