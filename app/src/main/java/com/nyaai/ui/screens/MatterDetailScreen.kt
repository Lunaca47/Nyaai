package com.nyaai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.data.local.RagDao
import com.nyaai.data.matter.*
import com.nyaai.data.verification.CitationBadgeHelper
import com.nyaai.data.verification.CitationBadgeType
import com.nyaai.ui.state.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatterDetailScreen(
    matterId: String,
    ragDao: RagDao?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateManager = remember(ragDao) { ragDao?.let { CaseStateManager(it) } }

    var matter by remember { mutableStateOf<Matter?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSyncing by remember { mutableStateOf(false) }

    fun loadMatter() {
        scope.launch {
            matter = stateManager?.getMatter(matterId)
        }
    }

    LaunchedEffect(matterId) {
        loadMatter()
    }

    val tabs = listOf(
        strings.tabOverview,
        strings.tabTimeline,
        strings.tabActionPlan,
        strings.tabEvidence,
        strings.tabCitations,
        strings.tabQuestions
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = matter?.title ?: "Matter Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = colors.onSurface
                        )
                        Text(
                            text = matter?.domain ?: "Legal Case",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                    }
                },
                actions = {
                    // Sync action
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val prefs = context.getSharedPreferences("nyaai_preferences", Context.MODE_PRIVATE)
                                val backendUrl = prefs.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
                                stateManager?.syncMatterWithBackend(matterId, backendUrl)
                                isSyncing = false
                                Toast.makeText(context, "Matter synchronized with server", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primary)
                        } else {
                            Icon(Icons.Outlined.CloudSync, contentDescription = "Sync", tint = colors.onSurfaceVariant)
                        }
                    }

                    // Share/Copy summary
                    IconButton(
                        onClick = {
                            val summary = matter?.let { m ->
                                "NYAAI CASE BRIEF: ${m.title}\nDomain: ${m.domain}\nJurisdiction: ${m.jurisdiction.state ?: "Central"}\nStage: ${m.proceduralStage}\nFacts: ${m.facts.size}\nTimeline Events: ${m.timeline.size}"
                            } ?: ""
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Matter Brief", summary))
                            Toast.makeText(context, "Matter brief copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Summary", tint = colors.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        val currentMatter = matter
        if (currentMatter == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Scrollable Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surface,
                    contentColor = colors.primary,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                // Tab Content View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> OverviewTab(matter = currentMatter, colors = colors)
                        1 -> TimelineTab(timeline = currentMatter.timeline, colors = colors)
                        2 -> ActionPlanTab(matter = currentMatter, colors = colors, onUpdate = { updated ->
                            scope.launch {
                                stateManager?.updateMatter(updated)
                                matter = updated
                            }
                        })
                        3 -> EvidenceTab(evidence = currentMatter.evidence, colors = colors)
                        4 -> CitationsTab(laws = currentMatter.applicableLaws, caseLaw = currentMatter.caseLaw, colors = colors)
                        5 -> OpenQuestionsTab(missing = currentMatter.missingInformation, questions = currentMatter.openQuestions, colors = colors)
                    }
                }
            }
        }
    }
}

// ── Tab 0: Overview & Facts ──────────────────────────────────────────────────
@Composable
private fun OverviewTab(matter: Matter, colors: ColorScheme) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Case Classification", fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DomainBadge(domain = matter.domain, colors = colors)
                        Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceVariant) {
                            Text(
                                "Stage: ${matter.proceduralStage.replace("_", " ")}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = colors.onSurface
                            )
                        }
                    }
                    Divider(color = colors.outline.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    Text("Jurisdiction: ${matter.jurisdiction.state ?: "Central / Pan-India"} (${matter.jurisdiction.confidence.name})", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        item {
            Text("Material Facts (${matter.facts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
        }

        if (matter.facts.isEmpty()) {
            item {
                Text("No structured facts extracted yet. Explain your dispute in chat to populate case facts.", color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(matter.facts) { fact ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProvenanceBadge(source = fact.source.name, colors = colors)
                            Text("Conf: ${fact.confidence}", fontSize = 11.sp, color = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(fact.statement, color = colors.onSurface, fontSize = 14.sp)
                    }
                }
            }
        }

        if (matter.options.isNotEmpty() || matter.risks.isNotEmpty()) {
            item {
                Text("Strategic Options & Risks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
            }
            items(matter.options) { opt ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("✓ ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    Text(opt, color = colors.onSurface, fontSize = 13.sp)
                }
            }
            items(matter.risks) { risk ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("⚠ ", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    Text(risk, color = colors.onSurface, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Tab 1: Timeline ──────────────────────────────────────────────────────────
@Composable
private fun TimelineTab(timeline: List<TimelineEvent>, colors: ColorScheme) {
    if (timeline.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chronological events recorded yet.", color = colors.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(timeline) { event ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val dateLabel = event.eventDate ?: "Date unrecorded"
                            Text(dateLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(event.description, fontSize = 14.sp, color = colors.onSurface)
                            if (!event.sourceFactId.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Linked to Fact: ${event.sourceFactId}", fontSize = 11.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab 2: Action Plan ───────────────────────────────────────────────────────
@Composable
private fun ActionPlanTab(matter: Matter, colors: ColorScheme, onUpdate: (Matter) -> Unit) {
    val actions = matter.nextActions
    if (actions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No action plan generated yet. Complete intake to generate step-by-step roadmap.", color = colors.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Action Roadmap & Procedural Steps", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
            }
            items(actions) { action ->
                var isCompleted by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCompleted = !isCompleted },
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { isCompleted = it },
                            colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = action,
                            fontSize = 14.sp,
                            color = if (isCompleted) colors.onSurfaceVariant else colors.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── Tab 3: Evidence ──────────────────────────────────────────────────────────
@Composable
private fun EvidenceTab(evidence: List<MatterEvidence>, colors: ColorScheme) {
    if (evidence.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.UploadFile, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No evidence attached to this matter yet.", color = colors.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(evidence) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.docType ?: "Legal Document", fontWeight = FontWeight.Bold, color = colors.onSurface)
                            val conf = item.classificationConfidence?.let { " (${(it * 100).toInt()}%)" } ?: ""
                            Text("Verified$conf", fontSize = 11.sp, color = colors.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(item.objectKey, fontSize = 12.sp, color = colors.onSurfaceVariant)
                        if (!item.extractedText.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Extracted text: ${item.extractedText.take(120)}...", fontSize = 12.sp, color = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ── Tab 4: Citations & Law ───────────────────────────────────────────────────
@Composable
private fun CitationsTab(
    laws: List<ApplicableLaw>,
    caseLaw: List<String> = emptyList(),
    colors: ColorScheme
) {
    if (laws.isEmpty() && caseLaw.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No statutory provisions or case law precedents linked yet.", color = colors.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (laws.isNotEmpty()) {
                item {
                    Text(
                        "Statutory Provisions (${laws.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.onBackground
                    )
                }
                items(laws) { law ->
                    val badge = CitationBadgeHelper.evaluateStatuteBadge(law)
                    val (containerColor, contentColor) = when (badge.type) {
                        CitationBadgeType.PASSED -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
                        CitationBadgeType.ANNOTATED_SUPERSEDED_PRECEDENT -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFD97706)
                        CitationBadgeType.ANNOTATED_REPEALED, CitationBadgeType.REJECTED_UNGROUNDED -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFEF4444)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${law.section}, ${law.act}",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = containerColor
                                ) {
                                    Text(
                                        badge.label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(badge.note, fontSize = 11.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }

            if (caseLaw.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Judicial Precedents & Case Law (${caseLaw.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.onBackground
                    )
                }
                items(caseLaw) { citation ->
                    val badge = CitationBadgeHelper.evaluatePrecedentBadge(citation)
                    val (containerColor, contentColor) = when (badge.type) {
                        CitationBadgeType.PASSED -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
                        CitationBadgeType.ANNOTATED_SUPERSEDED_PRECEDENT -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFD97706)
                        CitationBadgeType.ANNOTATED_REPEALED, CitationBadgeType.REJECTED_UNGROUNDED -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFEF4444)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    citation,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = containerColor
                                ) {
                                    Text(
                                        badge.label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(badge.note, fontSize = 11.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ── Tab 5: Open Questions & Missing Info ──────────────────────────────────────
@Composable
private fun OpenQuestionsTab(missing: List<String>, questions: List<String>, colors: ColorScheme) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (missing.isNotEmpty()) {
            item {
                Text("Missing Legal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
            }
            items(missing) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HelpOutline, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(item, fontSize = 13.sp, color = colors.onSurface)
                    }
                }
            }
        }

        if (questions.isNotEmpty()) {
            item {
                Text("Open Questions for Counsel / Evidence", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)
            }
            items(questions) { q ->
                Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.QuestionAnswer, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(q, fontSize = 13.sp, color = colors.onSurface)
                    }
                }
            }
        }

        if (missing.isEmpty() && questions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("All required statutory facts collected! Case intake is complete.", color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ProvenanceBadge(source: String, colors: ColorScheme) {
    val (label, bg, fg) = when (source.uppercase()) {
        "USER_STATED" -> Triple("Stated by User", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF3B82F6))
        "DOCUMENT_EXTRACTED" -> Triple("Extracted from Document", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
        "INFERRED" -> Triple("Legally Inferred", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B))
        else -> Triple(source, colors.surfaceVariant, colors.onSurfaceVariant)
    }

    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
