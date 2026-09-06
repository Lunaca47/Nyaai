package com.nyaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.data.local.RagDao
import com.nyaai.ui.state.LocalRagDao
import com.nyaai.ui.state.LocalStrings

@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()
    val strings     = LocalStrings.current
    val colors      = MaterialTheme.colorScheme
    val ragDao      = LocalRagDao.current
    
    var trainingCount by remember { mutableStateOf(0) }

    LaunchedEffect(ragDao) {
        if (ragDao != null) {
            ragDao.getTrainingCountFlow().collect { count ->
                trainingCount = count
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text       = strings.aboutTitle,
                color      = colors.onBackground,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = strings.aboutSubtitle,
                color    = colors.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Training Progress Card ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Psychology, 
                        null, 
                        tint = colors.primary, 
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "AI Training Progress", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp
                        )
                        Text(
                            "$trainingCount / 1000 legal queries learned",
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = trainingCount.toFloat() / 1000f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = colors.primary,
                            trackColor = colors.surface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Vision content ───────────────────────────────────────────────
            Text(
                text       = strings.visionTitle,
                color      = colors.onBackground,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text       = strings.visionPara1,
                color      = colors.onSurfaceVariant,
                fontSize   = 14.sp,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text       = strings.visionPara2,
                color      = colors.onSurfaceVariant,
                fontSize   = 14.sp,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // ── Footer ───────────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text       = "Nyaai App - v1.1.0 (Intelligence Patch)",
                    color      = colors.onSurfaceVariant,
                    fontSize   = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = strings.madeWithCare,
                    color      = Color(0xFF4A9E9E),
                    fontSize   = 14.sp,
                    fontStyle  = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
