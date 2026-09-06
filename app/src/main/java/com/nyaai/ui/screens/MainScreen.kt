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
import com.nyaai.ui.state.LocalStrings
import kotlinx.coroutines.launch

enum class MainTab { CHAT, ABOUT, SETTINGS }

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
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
    val bookmarks by (ragDao?.getAllBookmarksFlow() ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = emptyList())

    var chatSessions by remember { mutableStateOf<List<ChatSessionEntity>>(emptyList()) }

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
                Spacer(Modifier.height(48.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            strings.bookmarksTitle,
                            color = if (drawerSection == 1) colors.onPrimary else colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Divider(color = colors.outline, thickness = 1.dp, modifier  = Modifier.padding(horizontal = 16.dp))
                
                Column(modifier = Modifier.padding(16.dp)) {
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
                            ragDao = ragDao,
                            bottomPadding = if (isKeyboardOpen) 4.dp else 84.dp
                        )
                        MainTab.ABOUT    -> AboutScreen()
                        MainTab.SETTINGS -> SettingsScreen(ragDao = ragDao)
                    }
                }
                if (!isKeyboardOpen && selectedTab != MainTab.CHAT) {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }

            // ── Floating Bottom Nav Bar ───────────────────────────────────
            if (!isKeyboardOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(colors.surface)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
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
                        icon     = Icons.Outlined.Info,
                        label    = strings.navAbout,
                        selected = selectedTab == MainTab.ABOUT,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.ABOUT }
                    )
                    NavBarItem(
                        icon     = Icons.Outlined.Settings,
                        label    = strings.navSettings,
                        selected = selectedTab == MainTab.SETTINGS,
                        colors   = colors,
                        onClick  = { selectedTab = MainTab.SETTINGS }
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
