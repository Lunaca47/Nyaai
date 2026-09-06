package com.nyaai.ui.screens

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.data.local.RagDao
import com.nyaai.ui.state.*
import com.nyaai.ui.strings.AppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(ragDao: RagDao? = null, onBack: () -> Unit = {}) {
    val strings             = LocalStrings.current
    val currentTheme        = LocalAppTheme.current
    val updateTheme         = LocalThemeUpdater.current
    val currentLang         = LocalAppLanguage.current
    val updateLang          = LocalLanguageUpdater.current
    val aiResponseLang      = LocalAiResponseLanguage.current
    val updateAiLang        = LocalAiLanguageUpdater.current
    val notificationsEnabled = LocalNotificationEnabled.current
    val updateNotifications  = LocalNotificationUpdater.current
    val isLoggedIn          = LocalAuthState.current
    val updateAuth          = LocalAuthUpdater.current
    val colors              = MaterialTheme.colorScheme
    val context             = LocalContext.current
    val scope               = rememberCoroutineScope()

    val currentUser = remember(isLoggedIn) { FirebaseAuth.getInstance().currentUser }
    val displayName = if (isLoggedIn) {
        currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Signed In User"
    } else {
        "Guest User"
    }
    val displayEmail = if (isLoggedIn) {
        currentUser?.email?.takeIf { it.isNotBlank() } ?: "Account connected"
    } else {
        "Sign in to sync data"
    }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    var lowBandwidthEnabled by remember { mutableStateOf(false) }
    var showLanguageModal   by remember { mutableStateOf(false) }
    var showAiLangModal     by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }
    var showClearConfirm    by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp, bottom = 100.dp)
        ) {
            // ── Header with Back Navigation ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back to Chat",
                        tint = colors.onBackground
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = strings.settingsTitle,
                    color      = colors.onBackground,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Profile Card ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isLoggedIn) Color(0xFF9C27B0) else Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = avatarInitial,
                        color      = Color.White,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = displayName,
                        color      = colors.onSurface,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text     = displayEmail,
                        color    = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                if (isLoggedIn) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Google", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── APPEARANCE ───────────────────────────────────────────────────
            SettingsSectionLabel(strings.sectionAppearance, colors)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsIconBox(Icons.Outlined.Palette, colors)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text       = strings.theme,
                        color      = colors.onSurface,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Segmented theme selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(50.dp))
                ) {
                    AppTheme.values().forEach { option ->
                        val isSelected = currentTheme == option
                        val label = when (option) {
                            AppTheme.LIGHT  -> strings.themeLight
                            AppTheme.SYSTEM -> strings.themeSystem
                            AppTheme.DARK   -> strings.themeDark
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    if (isSelected) colors.surfaceVariant else Color.Transparent
                                )
                                .clickable { updateTheme(option) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = label,
                                color      = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                                fontSize   = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── PREFERENCES ──────────────────────────────────────────────────
            SettingsSectionLabel(strings.sectionPreferences, colors)
            Spacer(modifier = Modifier.height(8.dp))

            // Notifications Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .clickable { updateNotifications(!notificationsEnabled) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBox(Icons.Outlined.Notifications, colors)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = strings.notifications,
                        color      = colors.onSurface,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text       = strings.notificationsSubtitle,
                        color      = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { updateNotifications(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // App Language — opens modal
            SettingsRow(
                icon     = Icons.Outlined.Language,
                title    = strings.appLanguage,
                subtitle = currentLang.displayName,
                colors   = colors,
                onClick  = { showLanguageModal = true }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Low Bandwidth Mode with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBox(Icons.Outlined.Refresh, colors)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = strings.lowBandwidthMode,
                        color      = colors.onSurface,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text       = strings.lowBandwidthSubtitle,
                        color      = colors.onSurfaceVariant,
                        fontSize   = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked         = lowBandwidthEnabled,
                    onCheckedChange = { lowBandwidthEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = colors.onPrimary,
                        checkedTrackColor   = colors.primary,
                        uncheckedThumbColor = colors.onSurfaceVariant,
                        uncheckedTrackColor = colors.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                icon     = Icons.Outlined.Chat,
                title    = strings.aiResponseLanguage,
                subtitle = aiResponseLang.displayName,
                colors   = colors,
                onClick  = { showAiLangModal = true }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── PRIVACY & DATA ───────────────────────────────────────────────
            SettingsSectionLabel(strings.sectionPrivacy, colors)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon     = Icons.Outlined.History,
                title    = strings.clearChatHistory,
                subtitle = strings.clearChatSubtitle,
                colors   = colors,
                onClick  = { showClearConfirm = true }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                icon   = Icons.Outlined.Security,
                title  = strings.privacyPolicy,
                colors = colors,
                onClick = { showPrivacyDialog = true }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                icon     = Icons.Outlined.Logout,
                title    = if (isLoggedIn) strings.logOut else "Log In",
                subtitle = if (isLoggedIn) strings.logOutSubtitle else "Sign in to your account",
                colors   = colors,
                onClick  = { updateAuth(!isLoggedIn) }
            )
        }
    }

    // ── Clear History Confirmation ───────────────────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will permanently delete all your saved conversations from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ragDao?.clearAllHistory()
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        showClearConfirm = false
                    }
                }) {
                    Text("Clear All", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Privacy Policy Disclaimer Dialog ─────────────────────────────────────
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor   = colors.surface,
            titleContentColor = colors.onSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Outlined.Security,
                        contentDescription = null,
                        tint               = colors.primary,
                        modifier           = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = strings.privacyPolicy,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = colors.onSurface
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text       = strings.disclaimerTitle,
                        color      = colors.onSurfaceVariant,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = strings.disclaimerBody,
                        color      = colors.onSurface,
                        fontSize   = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        strings.disclaimerBullet1,
                        strings.disclaimerBullet2,
                        strings.disclaimerBullet3
                    ).forEach { bullet ->
                        Text(
                            text       = bullet,
                            color      = colors.onSurface,
                            fontSize   = 13.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    shape   = RoundedCornerShape(50.dp),
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7A9EB8),
                        contentColor   = Color.White
                    )
                ) {
                    Text(
                        text       = strings.iUnderstand,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                }
            }
        )
    }

    // ── App Language Bottom Sheet ─────────────────────────────────────────────────
    if (showLanguageModal) {
        LanguageModal(
            title = strings.langModalTitle,
            currentLang = currentLang,
            onLangSelected = { updateLang(it); showLanguageModal = false },
            colors = colors,
            onDismiss = { showLanguageModal = false }
        )
    }

    // ── AI Response Language Bottom Sheet ──────────────────────────────────────────
    if (showAiLangModal) {
        LanguageModal(
            title = strings.aiResponseLanguage,
            currentLang = aiResponseLang,
            onLangSelected = { updateAiLang(it); showAiLangModal = false },
            colors = colors,
            onDismiss = { showAiLangModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageModal(
    title: String,
    currentLang: AppLanguage,
    onLangSelected: (AppLanguage) -> Unit,
    colors: ColorScheme,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = colors.surface,
        contentColor     = colors.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(colors.outline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text       = title,
                color      = colors.onSurface,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            AppLanguage.values().forEach { lang ->
                val isSelected = lang == currentLang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLangSelected(lang) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick  = { onLangSelected(lang) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor   = colors.primary,
                            unselectedColor = colors.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = lang.displayName,
                        color      = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        fontSize   = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (lang != AppLanguage.values().last()) {
                    Divider(color = colors.outline, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String, colors: ColorScheme) {
    Text(
        text          = text,
        color         = colors.onSurfaceVariant,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun SettingsIconBox(icon: ImageVector, colors: ColorScheme) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = colors.onSurfaceVariant,
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    icon:     ImageVector,
    title:    String,
    subtitle: String?   = null,
    colors:   ColorScheme,
    onClick:  () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon, colors)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                color      = colors.onSurface,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = subtitle,
                    color    = colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = colors.onSurfaceVariant,
            modifier           = Modifier.size(20.dp)
        )
    }
}
