package com.nyaai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextOverflow
import com.nyaai.data.local.BookmarkEntity
import com.nyaai.data.local.ChatSessionEntity
import com.nyaai.data.local.RagDao
import com.nyaai.ui.state.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

enum class MainTab { CHAT, MATTERS, ABOUT, SETTINGS }


@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, ragDao: RagDao? = null) {
    var selectedTab by remember { mutableStateOf(MainTab.CHAT) }
    var currentSessionId by remember { mutableStateOf<Long?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val colors      = MaterialTheme.colorScheme
    val strings     = LocalStrings.current
    val context     = LocalContext.current
    val isKeyboardOpen = WindowInsets.isImeVisible
    var drawerSection by remember { mutableIntStateOf(0) }
    var openSosTrigger by remember { mutableStateOf(false) }
    val bookmarks by (ragDao?.getAllBookmarksFlow() ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

    var chatSessions by remember { mutableStateOf<List<ChatSessionEntity>>(emptyList()) }

    val isLoggedIn      = LocalAuthState.current
    val openLoginAction = LocalOpenLoginAction.current
    val updateAuth      = LocalAuthUpdater.current
    val auth            = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUser     = remember(isLoggedIn, auth.currentUser) { auth.currentUser }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Sign Out of Nyaai?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out? You can continue using the app as a guest.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        auth.signOut()
                        val prefs = context.getSharedPreferences("nyaai_preferences", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("guest_mode", true).apply()
                        updateAuth(false)
                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load sessions when drawer opens
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen && ragDao != null) {
            chatSessions = ragDao.getAllSessions()
        }
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.background,
                drawerContentColor   = colors.onBackground,
                modifier             = Modifier.width(320.dp)
            ) {
                Spacer(Modifier.height(40.dp))

                // ── Brand Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Gavel,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Nyaai",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                        Text(
                            "Your Legal Companion",
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // ── User Profile Status Card in Drawer ──
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentUser != null) {
                            val initial = (currentUser.displayName?.firstOrNull() ?: currentUser.email?.firstOrNull() ?: 'U').uppercaseChar()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initial.toString(), color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Citizen User",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    currentUser.email?.takeIf { it.isNotBlank() } ?: currentUser.phoneNumber ?: "Account Active",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { showLogoutConfirm = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Outlined.Logout, contentDescription = "Sign Out", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
                            }
                        } else {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Guest Mode", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = colors.onSurface)
                                Text("Tap to sign in / register", fontSize = 11.sp, color = colors.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    openLoginAction()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── App Navigation Links ──
                NavigationDrawerItem(
                    label = { Text(strings.navChat, fontWeight = FontWeight.Medium) },
                    selected = selectedTab == MainTab.CHAT,
                    onClick = {
                        selectedTab = MainTab.CHAT
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(strings.myMattersTitle, fontWeight = FontWeight.Medium) },
                    selected = selectedTab == MainTab.MATTERS,
                    onClick = {
                        selectedTab = MainTab.MATTERS
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚨 Legal SOS Helplines", fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                            Spacer(Modifier.weight(1f))
                            Badge(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)) { Text("17 Helplines") }
                        }
                    },
                    selected = false,
                    onClick = {
                        selectedTab = MainTab.CHAT
                        openSosTrigger = true
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFFDC2626)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(strings.bookmarksTitle, fontWeight = FontWeight.Medium) },
                    selected = drawerSection == 1,
                    badge = {
                        if (bookmarks.isNotEmpty()) {
                            Badge { Text("${bookmarks.size}") }
                        }
                    },
                    onClick = {
                        drawerSection = 1
                    },
                    icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(strings.navSettings, fontWeight = FontWeight.Medium) },
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = {
                        selectedTab = MainTab.SETTINGS
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(strings.navAbout, fontWeight = FontWeight.Medium) },
                    selected = selectedTab == MainTab.ABOUT,
                    onClick = {
                        selectedTab = MainTab.ABOUT
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Divider(color = colors.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // ── History / Bookmarks Toggle ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (drawerSection == 0) colors.primary else Color.Transparent)
                            .clickable { drawerSection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.chatHistory,
                            color = if (drawerSection == 0) colors.onPrimary else colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (drawerSection == 1) colors.primary else Color.Transparent)
                            .clickable { drawerSection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.bookmarksTitle + if (bookmarks.isNotEmpty()) " (${bookmarks.size})" else "",
                            color = if (drawerSection == 1) colors.onPrimary else colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Divider(color = colors.outline.copy(alpha = 0.3f), thickness = 1.dp, modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (drawerSection == 0) {
                        if (chatSessions.isEmpty()) {
                            Text("No history yet", color = colors.onSurfaceVariant, fontSize = 14.sp)
                        }
                        chatSessions.forEach { session ->
                            ChatHistoryItem(
                                session = session,
                                colors = colors,
                                onSelect = {
                                    currentSessionId = session.sessionId
                                    selectedTab = MainTab.CHAT
                                    scope.launch { drawerState.close() }
                                },
                                onDelete = {
                                    scope.launch {
                                        ragDao?.deleteSession(session.sessionId)
                                        chatSessions = ragDao?.getAllSessions() ?: emptyList()
                                        if (currentSessionId == session.sessionId) currentSessionId = null
                                    }
                                },
                                onCopy = {
                                    scope.launch {
                                        val messages = ragDao?.getMessagesForSession(session.sessionId)
                                        val fullChat = messages?.joinToString("\n") { (if(it.isUser) "User: " else "Nyaai: ") + it.text }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Nyaai Chat", fullChat))
                                        Toast.makeText(context, "Chat copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShare = {
                                    scope.launch {
                                        val messages = ragDao?.getMessagesForSession(session.sessionId)
                                        val fullChat = messages?.joinToString("\n") { (if(it.isUser) "User: " else "Nyaai: ") + it.text }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, fullChat)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Chat"))
                                    }
                                }
                            )
                        }
                    } else {
                        if (bookmarks.isEmpty()) {
                            Text(strings.noBookmarksYet, color = colors.onSurfaceVariant, fontSize = 14.sp)
                        }
                        bookmarks.forEach { bookmark ->
                            BookmarkHistoryItem(
                                bookmark = bookmark,
                                colors = colors,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Legal Reference", "${bookmark.title}\n\n${bookmark.content}"))
                                    Toast.makeText(context, "Bookmark copied", Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${bookmark.title}\n\n${bookmark.content}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Bookmark"))
                                },
                                onDelete = {
                                    scope.launch {
                                        ragDao?.deleteBookmark(bookmark.id)
                                        Toast.makeText(context, strings.removedBookmarkToast, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        MainTab.CHAT     -> ChatScreen(
                            sessionId = currentSessionId,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onNewChat = { currentSessionId = null },
                            onOpenSettings = { selectedTab = MainTab.SETTINGS },
                            onOpenBookmarks = {
                                drawerSection = 1
                                scope.launch { drawerState.open() }
                            },
                            openSosTrigger = openSosTrigger,
                            onSosTriggerHandled = { openSosTrigger = false },
                            ragDao = ragDao,
                            bottomPadding = if (isKeyboardOpen) 4.dp else 84.dp
                        )
                        MainTab.MATTERS  -> MattersScreen(
                            ragDao = ragDao,
                            onOpenMatter = { matterId ->
                                navController.navigate("matter_detail/$matterId")
                            },
                            onStartNewIntake = {
                                currentSessionId = null
                                selectedTab = MainTab.CHAT
                            }
                        )
                        MainTab.ABOUT    -> AboutScreen(onBack = { selectedTab = MainTab.CHAT })
                        MainTab.SETTINGS -> SettingsScreen(ragDao = ragDao, onBack = { selectedTab = MainTab.CHAT })
                    }
                }
                if (!isKeyboardOpen && selectedTab != MainTab.CHAT && selectedTab != MainTab.MATTERS) {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }

            // ── Floating Bottom Nav Bar ───────────────────────────────────
            if (!isKeyboardOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    NavBarItem(
                        icon     = Icons.Outlined.ChatBubbleOutline,
                        label    = strings.navChat,
                        selected = selectedTab == MainTab.CHAT,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.CHAT }
                    )
                    NavBarItem(
                        icon     = Icons.Outlined.Folder,
                        label    = strings.myMattersTitle,
                        selected = selectedTab == MainTab.MATTERS,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.MATTERS }
                    )
                    NavBarItem(
                        icon     = Icons.Outlined.BookmarkBorder,
                        label    = strings.bookmarksTitle,
                        selected = drawerState.isOpen && drawerSection == 1,
                        colors   = colors,
                        onClick  = {
                            drawerSection = 1
                            scope.launch { drawerState.open() }
                        }
                    )
                    NavBarItem(
                        icon     = Icons.Outlined.Settings,
                        label    = strings.navSettings,
                        selected = selectedTab == MainTab.SETTINGS,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.SETTINGS }
                    )
                    NavBarItem(
                        icon     = Icons.Outlined.Info,
                        label    = strings.navAbout,
                        selected = selectedTab == MainTab.ABOUT,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.ABOUT }
                    )
                }
            }
        }

    }
}
}

@Composable
private fun ChatHistoryItem(
    session: ChatSessionEntity,
    colors: ColorScheme,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint               = colors.onSurfaceVariant,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(session.title, color = colors.onSurfaceVariant, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
        
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.MoreVert, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy Chat") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                    onClick = { showMenu = false; onCopy() }
                )
                DropdownMenuItem(
                    text = { Text("Share Chat") },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick = { showMenu = false; onShare() }
                )
                DropdownMenuItem(
                    text = { Text("Delete Chat", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color.Red) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon:     ImageVector,
    label:    String,
    selected: Boolean,
    colors:   ColorScheme,
    onClick:  () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (selected) 46.dp else 42.dp)
            .clip(CircleShape)
            .background(if (selected) colors.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (selected) colors.onSurface else colors.onSurfaceVariant,
            modifier           = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun BookmarkHistoryItem(
    bookmark: BookmarkEntity,
    colors: ColorScheme,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = bookmark.content.lines().firstOrNull { it.isNotBlank() } ?: "",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.MoreVert, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                    onClick = { showMenu = false; onCopy() }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick = { showMenu = false; onShare() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color.Red) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}
