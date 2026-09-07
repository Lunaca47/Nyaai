package com.nyaai.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.data.local.BookmarkEntity
import com.nyaai.data.local.ChatSessionEntity
import com.nyaai.data.local.DocumentScannerService
import com.nyaai.data.local.MessageEntity
import com.nyaai.data.local.RagDao
import com.nyaai.ui.state.*
import kotlinx.coroutines.launch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
    val feedback: String? = null,
    val confidence: Double? = null,
    val isBookmarked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: Long? = null,
    onMenuClick: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    ragDao: RagDao? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var inputText       by remember { mutableStateOf("") }
    var activeSessionId by remember { mutableStateOf(sessionId) }
    val messages        = remember { mutableStateListOf<ChatMessage>() }
    
    var showSosSheet    by remember { mutableStateOf(false) }

    val strings         = LocalStrings.current
    val currentLang     = LocalAppLanguage.current
    val aiResponseLang  = LocalAiResponseLanguage.current
    val colors          = MaterialTheme.colorScheme
    val context         = LocalContext.current
    val aiService       = LocalAiService.current
    val scope           = rememberCoroutineScope()
    val listState       = rememberLazyListState()

    val scannerService  = remember { DocumentScannerService(context) }
    var isScanning      by remember { mutableStateOf(false) }
    var isListening     by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        activeSessionId = sessionId
        messages.clear()
        if (sessionId != null && ragDao != null) {
            val history = ragDao.getMessagesForSession(sessionId)
            val bookmarks = ragDao.getAllBookmarks()
            val bookmarkedContents = bookmarks.map { it.content }.toSet()
            messages.addAll(history.map {
                ChatMessage(
                    id = it.id,
                    text = it.text,
                    isUser = it.isUser,
                    feedback = it.feedback,
                    confidence = it.confidence,
                    isBookmarked = it.text in bookmarkedContents
                )
            })
        }
    }

    // ── Export Launchers ──────────────────────────────────────────────────────
    val exportTextLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(messages.joinToString("\n\n") { "${if (it.isUser) "User" else "Nyaai"}: ${it.text}" }.toByteArray())
                    Toast.makeText(context, "Chat exported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show() }
        }
    }

    val attachFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { docUri ->
        if (docUri != null) {
            scope.launch {
                isScanning = true
                Toast.makeText(context, strings.scanningDocument, Toast.LENGTH_SHORT).show()
                val result = scannerService.extractTextFromUri(docUri)
                isScanning = false
                result.onSuccess { prompt ->
                    inputText = prompt
                    Toast.makeText(context, "Document text extracted!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Scan error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        val document = PDDocument()
                        val page = PDPage()
                        document.addPage(page)
                        val cs = PDPageContentStream(document, page)
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                        cs.setLeading(20f)
                        cs.newLineAtOffset(50f, 750f)
                        cs.showText("Nyaai Chat Export")
                        cs.newLine()
                        cs.setFont(PDType1Font.HELVETICA, 10f)
                        cs.setLeading(14f)

                        messages.take(45).forEach { msg ->
                            val prefix = if (msg.isUser) "User: " else "Nyaai: "
                            val cleanText = (prefix + msg.text.replace("\n", " "))
                                .map { ch -> if (ch.code in 32..126) ch else '?' }
                                .joinToString("")
                            cs.showText(cleanText.take(85))
                            cs.newLine()
                        }
                        cs.endText()
                        cs.close()
                        document.save(os)
                        document.close()
                        Toast.makeText(context, "PDF Exported", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { 
                    Log.e("ChatScreen", "PDF export error: ${e.message}")
                    Toast.makeText(context, "PDF failed: ${e.message}", Toast.LENGTH_SHORT).show() 
                }
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spoken = results[0]
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    fun launchVoice() {
        val voiceLangCode = when (currentLang) {
            AppLanguage.ENGLISH -> "en-IN"
            AppLanguage.HINDI   -> "hi-IN"
            AppLanguage.BENGALI -> "bn-IN"
            AppLanguage.TELUGU  -> "te-IN"
            AppLanguage.TAMIL   -> "ta-IN"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLangCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.chatInputHint)
        }
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        if (activities.isEmpty()) {
            Toast.makeText(context, "Voice recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }
        try {
            isListening = true
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Voice input error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Outlined.Menu, "Menu", tint = colors.onBackground, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Outlined.Balance, "Logo", tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nyaai", color = colors.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Text("✳", color = Color(0xFFE53935), fontSize = 18.sp, modifier = Modifier.clickable { showSosSheet = true })
                Spacer(Modifier.weight(1f))
                
                // Bookmarks Button
                IconButton(onClick = onOpenBookmarks) {
                    Icon(Icons.Outlined.BookmarkBorder, "Bookmarks", tint = colors.onSurfaceVariant)
                }

                // Settings Button
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, "Settings", tint = colors.onSurfaceVariant)
                }

                // Export & Bookmark Entire Chat
                var showExportMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showExportMenu = true }) { Icon(Icons.Outlined.IosShare, "Export", tint = colors.onSurfaceVariant) }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("🔖 Bookmark Entire Chat") },
                            onClick = {
                                showExportMenu = false
                                if (messages.isEmpty()) {
                                    Toast.makeText(context, "No messages to bookmark", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        val firstQuery = messages.firstOrNull { it.isUser }?.text?.take(40) ?: "Legal Chat"
                                        val fullChat = messages.joinToString("\n\n") { (if(it.isUser) "User: " else "Nyaai: ") + it.text }
                                        ragDao?.insertBookmark(BookmarkEntity(title = firstQuery, content = fullChat, source = "Chat Thread"))
                                        Toast.makeText(context, "Entire chat bookmarked!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(text = { Text("Export as Text") }, onClick = { showExportMenu = false; exportTextLauncher.launch("Nyaai_Chat_${System.currentTimeMillis()}.txt") })
                        DropdownMenuItem(text = { Text("Export as PDF") }, onClick = { showExportMenu = false; exportPdfLauncher.launch("Nyaai_Chat_${System.currentTimeMillis()}.pdf") })
                    }
                }
                IconButton(onClick = { messages.clear(); activeSessionId = null; onNewChat() }) { Icon(Icons.Outlined.Edit, "New chat", tint = colors.onSurfaceVariant) }
            }

            if (messages.isEmpty()) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(strings.whatCanIHelpWith, color = colors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(28.dp))
                    listOf(strings.scanDocument, strings.knowYourFIR, strings.draftRentAgreement, strings.consumerRightsOverview).forEach { actionText ->
                        QuickActionChip(actionText, colors) { chip ->
                            if (chip == strings.scanDocument) {
                                attachFileLauncher.launch("*/*")
                            } else {
                                inputText = chip
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), state = listState, reverseLayout = true) {
                    items(items = messages.reversed()) { msg ->
                        ChatBubble(
                            message = msg,
                            colors = colors,
                            onFeedback = { feedback ->
                                scope.launch {
                                    ragDao?.updateMessageFeedback(msg.id, feedback)
                                    val idx = messages.indexOfFirst { it.id == msg.id }
                                    if (idx != -1) messages[idx] = messages[idx].copy(feedback = feedback)
                                    Toast.makeText(context, "Thanks for the feedback!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleBookmark = {
                                scope.launch {
                                    if (ragDao != null) {
                                        val exists = ragDao.isBookmarked(msg.text) > 0
                                        if (exists) {
                                            ragDao.deleteBookmarkByContent(msg.text)
                                            val idx = messages.indexOfFirst { it.id == msg.id }
                                            if (idx != -1) messages[idx] = messages[idx].copy(isBookmarked = false)
                                            Toast.makeText(context, strings.removedBookmarkToast, Toast.LENGTH_SHORT).show()
                                        } else {
                                            val titleSnippet = msg.text.lines().firstOrNull { it.isNotBlank() }?.take(40) ?: "Legal Reference"
                                            ragDao.insertBookmark(BookmarkEntity(title = titleSnippet, content = msg.text, source = "AI Answer"))
                                            val idx = messages.indexOfFirst { it.id == msg.id }
                                            if (idx != -1) messages[idx] = messages[idx].copy(isBookmarked = true)
                                            Toast.makeText(context, strings.bookmarkedToast, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(bottom = bottomPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.dp, colors.outline, RoundedCornerShape(50.dp))
                    .background(colors.surfaceVariant)
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                } else {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Attach File",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp).clickable { attachFileLauncher.launch("*/*") }
                    )
                }
                Spacer(Modifier.width(8.dp))
                BasicTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f), textStyle = TextStyle(color = colors.onBackground, fontSize = 15.sp), cursorBrush = SolidColor(colors.primary), singleLine = true, decorationBox = { inner -> if (inputText.isEmpty()) Text(strings.chatInputHint, color = colors.onSurfaceVariant, fontSize = 15.sp); inner() })
                IconButton(onClick = { launchVoice() }) {
                    Icon(
                        if (isListening) Icons.Filled.Mic else Icons.Outlined.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color(0xFFE53935) else colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                val canSend = inputText.isNotBlank() && !isScanning
                IconButton(onClick = { 
                    val query = inputText.trim(); inputText = ""
                    val userMsg = ChatMessage(text = query, isUser = true); messages.add(userMsg)
                    val loadingMsg = ChatMessage(text = "", isUser = false, isTyping = true); messages.add(loadingMsg)
                    scope.launch {
                        if (activeSessionId == null && ragDao != null) activeSessionId = ragDao.createSession(ChatSessionEntity(title = query.take(30)))
                        activeSessionId?.let { id ->
                            ragDao?.insertMessage(MessageEntity(sessionId = id, text = query, isUser = true))
                            try {
                                val result = aiService?.generateAnswer(query, aiResponseLang)
                                val answer = result?.first ?: "Service Error"
                                val confidence = result?.second ?: 0.0
                                val msgId = ragDao?.insertMessage(MessageEntity(sessionId = id, text = answer, isUser = false, confidence = confidence)) ?: 0L
                                val idx = messages.indexOf(loadingMsg)
                                if (idx != -1) messages[idx] = ChatMessage(id = msgId, text = answer, isUser = false, confidence = confidence, isBookmarked = false)
                            } catch (e: Exception) { val idx = messages.indexOf(loadingMsg); if (idx != -1) messages[idx] = ChatMessage(text = "Error: ${e.message}", isUser = false) }
                        }
                    }
                }, enabled = canSend, modifier = Modifier.size(36.dp).clip(CircleShape).background(if (canSend) colors.primary else colors.outline)) { Icon(Icons.Outlined.Send, null, tint = if (canSend) colors.onPrimary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showSosSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSosSheet = false },
            containerColor = colors.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚨", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.legalSosTitle,
                            color = colors.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Direct statutory emergency helplines & citizen rights",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "EMERGENCY HELPLINES (TAP TO CALL)",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))

                emergencyHelplines.forEach { helpline ->
                    HelplineCard(helpline = helpline, colors = colors, context = context)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "KNOW YOUR RIGHTS (CITIZEN GUIDES)",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))

                citizenRightGuides.forEach { guide ->
                    CitizenRightCard(guide = guide, colors = colors)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    colors: ColorScheme,
    onFeedback: (String) -> Unit,
    onToggleBookmark: () -> Unit
) {
    val isUser = message.isUser
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.Top) {
            if (!isUser) { Box(Modifier.size(32.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Balance, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp)) }; Spacer(Modifier.width(8.dp)) }
            Box(modifier = Modifier.weight(1f, false).clip(RoundedCornerShape(16.dp, 16.dp, if (isUser) 16.dp else 4.dp, if (isUser) 4.dp else 16.dp)).background(if (isUser) colors.primary else colors.surfaceVariant).padding(12.dp)) {
                if (message.isTyping) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
                else {
                    Column {
                        Text(message.text, color = if (isUser) colors.onPrimary else colors.onSurface, fontSize = 15.sp)
                        if (!isUser && message.confidence != null) {
                            Spacer(Modifier.height(4.dp))
                            val percentage = (message.confidence * 100).toInt()
                            val confidenceColor = when {
                                percentage >= 80 -> Color(0xFF4CAF50)
                                percentage >= 50 -> Color(0xFFFFC107)
                                else -> Color(0xFFE53935)
                            }
                            Text(
                                "Confidence: $percentage%", 
                                color = confidenceColor, 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        if (!isUser && !message.isTyping && message.id != 0L) {
            Row(
                modifier = Modifier.padding(start = 40.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedbackButton("Good", message.feedback == "GOOD", colors) { onFeedback("GOOD") }
                Spacer(Modifier.width(6.dp))
                FeedbackButton("Average", message.feedback == "AVERAGE", colors) { onFeedback("AVERAGE") }
                Spacer(Modifier.width(6.dp))
                FeedbackButton("Poor", message.feedback == "POOR", colors) { onFeedback("POOR") }
                Spacer(Modifier.width(10.dp))
                Surface(
                    onClick = onToggleBookmark,
                    shape = RoundedCornerShape(50.dp),
                    color = if (message.isBookmarked) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (message.isBookmarked) colors.primary else colors.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            if (message.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (message.isBookmarked) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (message.isBookmarked) "Saved ✓" else "Bookmark",
                            color = if (message.isBookmarked) colors.primary else colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackButton(label: String, isSelected: Boolean, colors: ColorScheme, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = if (isSelected) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(label, fontSize = 10.sp, color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant)
        }
    }
}

data class HelplineEntry(
    val name: String,
    val number: String,
    val description: String
)

data class CitizenRightGuide(
    val title: String,
    val icon: String,
    val points: List<String>
)

private val emergencyHelplines = listOf(
    HelplineEntry("National Legal Aid (NALSA)", "15100", "Free legal aid representation for eligible citizens & undertrials"),
    HelplineEntry("National Consumer Helpline", "1915", "Consumer dispute resolution, defective products & unfair trade"),
    HelplineEntry("National Cybercrime Portal", "1930", "Financial cyber fraud, phishing & online offense grievance"),
    HelplineEntry("Women in Distress Helpline", "1091", "24/7 National Commission for Women emergency assistance"),
    HelplineEntry("NCW Women Helpline / WhatsApp", "7827170170", "National Commission for Women direct support & complaint line"),
    HelplineEntry("Childline Emergency", "1098", "Child protection, safety, rescue & POCSO grievance redressal"),
    HelplineEntry("Senior Citizen Helpline", "14567", "Elder line for welfare, maintenance & legal protection"),
    HelplineEntry("Emergency Response System (ERSS)", "112", "Unified national emergency number for police, fire & medical support")
)

private val citizenRightGuides = listOf(
    CitizenRightGuide(
        title = "Rights Upon Arrest (D.K. Basu Guidelines)",
        icon = "⚖️",
        points = listOf(
            "Right to know the full grounds of arrest and whether the offence is bailable or non-bailable (Section 47 BNSS).",
            "Right to have a relative or friend informed of the arrest immediately.",
            "Mandatory medical examination by an authorized medical officer within 48 hours.",
            "Right to consult an advocate of your choice during interrogation."
        )
    ),
    CitizenRightGuide(
        title = "Zero FIR Provision (Section 173 BNSS)",
        icon = "📝",
        points = listOf(
            "A police station cannot refuse to register an FIR on the grounds of territorial jurisdiction.",
            "A Zero FIR is registered and subsequently transferred to the competent jurisdiction police station.",
            "Audio-video recording of search and seizure operations is now mandated under BNSS 2023."
        )
    ),
    CitizenRightGuide(
        title = "Free Legal Aid (Article 39A)",
        icon = "🛡️",
        points = listOf(
            "Article 39A mandates the State to secure equal justice and free legal aid for all citizens.",
            "Under the Legal Services Authorities Act, women, children, undertrials, and individuals with annual income below statutory limits are entitled to free representation in all courts."
        )
    )
)

@Composable
private fun HelplineCard(
    helpline: HelplineEntry,
    colors: ColorScheme,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.7f))
            .clickable {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.number}")))
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = helpline.name,
                color = colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = helpline.description,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Dial ${helpline.number}",
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        FilledIconButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.number}")))
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(Icons.Outlined.Call, contentDescription = "Call ${helpline.name}", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CitizenRightCard(
    guide: CitizenRightGuide,
    colors: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(guide.icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    guide.title,
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            guide.points.forEach { pt ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        pt,
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(label: String, colors: ColorScheme, onClick: (String) -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).border(1.dp, colors.outline, RoundedCornerShape(50.dp)).clickable { onClick(label) }.padding(horizontal = 20.dp, vertical = 10.dp)) { Text(label, color = colors.onSurface, fontSize = 14.sp) }
}
