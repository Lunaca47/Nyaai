package com.nyaai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.ui.state.LocalStrings

// Fallback elegant cursive look via serif + italic
val scriptFamily = FontFamily.Serif

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    val strings = LocalStrings.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // WelcomeScreen keeps a dark gradient regardless of theme — it's dramatic onboarding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1C2E),
                        Color(0xFF1A2A40),
                        Color(0xFF0A1220)
                    )
                )
            )
    ) {
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = strings.welcomeTo,
                color         = Color(0xFFB8CDD8),
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text          = "Nyaai",
                color         = Color(0xFFDAE8F0),
                fontSize      = 56.sp,
                fontFamily    = scriptFamily,
                fontStyle     = FontStyle.Italic,
                fontWeight    = FontWeight.Light,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text       = strings.tagline,
                color      = Color(0xFF8FAAB8),
                fontSize   = 16.sp,
                fontStyle  = FontStyle.Italic,
                fontFamily = scriptFamily,
                fontWeight = FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .scale(scale)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFB8CDD8).copy(alpha = 0.85f))
                .clickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.ArrowForwardIos,
                contentDescription = "Next",
                tint               = Color(0xFF1A2A40),
                modifier           = Modifier.size(24.dp)
            )
        }
    }
}
