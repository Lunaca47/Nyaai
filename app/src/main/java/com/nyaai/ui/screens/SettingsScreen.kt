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
    val openLoginAction     = LocalOpenLoginAction.current
    val logOutAction        = LocalLogOutAction.current
    val colors              = MaterialTheme.colorScheme
    val context             = LocalContext.current
    val scope               = rememberCoroutineScope()
    val prefs               = remember { context.getSharedPreferences("nyaai_preferences", android.content.Context.MODE_PRIVATE) }

    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember(isLoggedIn, auth.currentUser) { auth.currentUser }
    val isRealUserSignedIn = currentUser != null

    val displayName = if (isRealUserSignedIn) {
        currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Citizen User"
    } else {
        "Guest User"
    }
    val displayEmail = if (isRealUserSignedIn) {
        currentUser?.email?.takeIf { it.isNotBlank() } 
            ?: currentUser?.phoneNumber?.takeIf { it.isNotBlank() } 
            ?: "Account connected"
    } else {
        "Browsing as Guest • Tap to Sign In"
    }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    val providerBadge = if (isRealUserSignedIn) {
        val providers = currentUser?.providerData?.map { it.providerId } ?: emptyList()
        when {
            providers.contains("google.com") -> "Google"
            providers.contains("phone") -> "Phone"
            else -> "Verified"
        }
    } else {
        "Guest"
    }

    var lowBandwidthEnabled by remember { mutableStateOf(false) }
    var showLanguageModal   by remember { mutableStateOf(false) }
    var showAiLangModal     by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }
    var showClearConfirm    by remember { mutableStateOf(false) }
    var showLogoutConfirm   by remember { mutableStateOf(false) }
    var showAccountSheet    by remember { mutableStateOf(false) }
    var customApiKeyInput   by remember { mutableStateOf(prefs.getString("custom_gemini_api_key", "") ?: "") }
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

            // ── Profile / Account Card ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .clickable { 
                        if (isRealUserSignedIn) {
                            showAccountSheet = true
                        } else {
                            openLoginAction()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isRealUserSignedIn) Color(0xFF7C4DFF) else colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRealUserSignedIn) {
                        Text(
                            text       = avatarInitial,
                            color      = Color.White,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = displayName,
                            color      = colors.onSurface,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isRealUserSignedIn) colors.primaryContainer else colors.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = providerBadge, 
                                color = if (isRealUserSignedIn) colors.onPrimaryContainer else colors.onSurfaceVariant, 
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = displayEmail,
                        color    = if (isRealUserSignedIn) colors.onSurfaceVariant else colors.primary,
                        fontSize = 12.sp,
                        fontWeight = if (isRealUserSignedIn) FontWeight.Normal else FontWeight.Medium
                    )
                }

                if (!isRealUserSignedIn) {
                    FilledTonalButton(
                        onClick = { openLoginAction() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = { showAccountSheet = true }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Manage Account", tint = colors.onSurfaceVariant)
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
            SettingsRow(
                icon     = Icons.Outlined.ManageAccounts,
                title    = "Account & API Settings",
                subtitle = if (isRealUserSignedIn) "Manage account, provider, and Gemini key" else "Configure custom Gemini API key",
                colors   = colors,
                onClick  = { showAccountSheet = true }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                icon     = if (isRealUserSignedIn) Icons.Outlined.Logout else Icons.Outlined.Login,
                title    = if (isRealUserSignedIn) strings.logOut else "Log In / Register",
                subtitle = if (isRealUserSignedIn) strings.logOutSubtitle else "Sign in with Google or Phone",
                colors   = colors,
                onClick  = { 
                    if (isRealUserSignedIn) {
                        showLogoutConfirm = true
                    } else {
                        openLoginAction()
                    }
                }
            )
        }
    }

    // ── Log Out Confirmation Dialog ──────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out of Nyaai?") },
            text = { Text("Are you sure you want to log out? You can sign back in anytime with your phone or Google account.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    auth.signOut()
                    prefs.edit().putBoolean("guest_mode", false).apply()
                    updateAuth(false)
                    logOutAction()
                }) {
                    Text("Log Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Account Details & API Key Bottom Sheet ──────────────────────────────
    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ManageAccounts, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Account & Preferences", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                }
                Spacer(Modifier.height(18.dp))

                // Account card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isRealUserSignedIn) Color(0xFF7C4DFF) else Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatarInitial, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
                                Text(displayEmail, fontSize = 13.sp, color = colors.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Divider(color = colors.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sign-in Method", fontSize = 13.sp, color = colors.onSurfaceVariant)
                            Text(if (isRealUserSignedIn) "$providerBadge Authentication" else "Guest Session", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.onSurface)
                        }
                        if (isRealUserSignedIn && currentUser?.uid != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("User ID", fontSize = 13.sp, color = colors.onSurfaceVariant)
                                Text(currentUser.uid.take(12) + "...", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Custom Gemini API Key configuration
                Text("Custom Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Optional: Enter your own Google Gemini API key to avoid rate limits on high usage.", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = customApiKeyInput,
                    onValueChange = { customApiKeyInput = it },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (customApiKeyInput.isNotBlank()) {
                        TextButton(onClick = {
                            customApiKeyInput = ""
                            prefs.edit().remove("custom_gemini_api_key").apply()
                            Toast.makeText(context, "Custom API key reset to default", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Reset")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            prefs.edit().putString("custom_gemini_api_key", customApiKeyInput.trim()).apply()
                            Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Key")
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (isRealUserSignedIn) {
                    OutlinedButton(
                        onClick = {
                            showAccountSheet = false
                            showLogoutConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Out of Account", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            showAccountSheet = false
                            openLoginAction()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log In / Create Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
