package com.nyaai.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyaai.data.local.RagDao
import com.nyaai.data.matter.CaseStateManager
import com.nyaai.data.matter.Matter
import com.nyaai.ui.state.LocalStrings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MattersScreen(
    ragDao: RagDao?,
    onOpenMatter: (String) -> Unit,
    onStartNewIntake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateManager = remember(ragDao) { ragDao?.let { CaseStateManager(it) } }

    var matters by remember { mutableStateOf<List<Matter>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomainFilter by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshMatters() {
        scope.launch {
            isLoading = true
            matters = stateManager?.getAllMatters() ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshMatters()
    }

    val domains = listOf("All", "Tenancy", "Cheque Bounce", "Hit & Run", "FIR Refusal", "Cyber Fraud", "Consumer", "Employment", "Domestic Violence", "General")

    val filteredMatters = remember(matters, searchQuery, selectedDomainFilter) {
        matters.filter { m ->
            val matchesQuery = searchQuery.isBlank() ||
                    m.title.contains(searchQuery, ignoreCase = true) ||
                    m.domain.contains(searchQuery, ignoreCase = true) ||
                    (m.jurisdiction.state?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchesDomain = selectedDomainFilter == "All" ||
                    m.domain.contains(selectedDomainFilter, ignoreCase = true)
            matchesQuery && matchesDomain
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartNewIntake,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(28.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "New Case") },
                text = { Text(strings.whatHappenedAction, fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.myMattersTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                    Text(
                        text = "Active legal cases, evidence & procedural roadmaps",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.onSurfaceVariant
                        )
                    )
                }

                IconButton(onClick = { refreshMatters() }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Matters",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchMattersHint, color = colors.onSurfaceVariant.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, null, tint = colors.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline
                ),
                singleLine = true
            )

            // Domain Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(domains) { domain ->
                    val isSelected = selectedDomainFilter == domain
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDomainFilter = domain },
                        label = { Text(domain, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                            containerColor = colors.surface,
                            labelColor = colors.onSurface
                        )
                    )

                }
            }

            Spacer(Modifier.height(8.dp))

            // Matters List or Empty State
            if (filteredMatters.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = colors.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedDomainFilter != "All") "No matching matters found" else strings.noMattersYet,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.noMattersDesc,
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onStartNewIntake,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.startCaseIntake)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredMatters, key = { it.matterId }) { matter ->
                        MatterCard(
                            matter = matter,
                            colors = colors,
                            strings = strings,
                            onClick = { onOpenMatter(matter.matterId) },
                            onDelete = {
                                scope.launch {
                                    stateManager?.deleteMatter(matter.matterId)
                                    refreshMatters()
                                    Toast.makeText(context, "Matter removed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatterCard(
    matter: Matter,
    colors: ColorScheme,
    strings: com.nyaai.ui.strings.AppStrings = LocalStrings.current,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(matter.updatedAt) { dateFormat.format(Date(matter.updatedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Domain chip & Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DomainBadge(domain = matter.domain, colors = colors)

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open Workspace") },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.deleteMatterAction, color = Color(0xFFDC2626)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFDC2626)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = matter.title.ifBlank { "Untitled Legal Case" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Badges Row: Jurisdiction & Stage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jurisdiction badge
                val stateText = matter.jurisdiction.state ?: "National (Central)"
                val confText = matter.jurisdiction.confidence.name.lowercase().replaceFirstChar { it.uppercase() }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$stateText ($confText)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                }

                // Stage badge
                val stageColor = when (matter.proceduralStage.lowercase()) {
                    "completed", "action_plan_ready" -> Color(0xFF10B981)
                    "court_proceedings" -> Color(0xFF8B5CF6)
                    "intake" -> Color(0xFFF59E0B)
                    else -> colors.primary
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stageColor.copy(alpha = 0.15f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = matter.proceduralStage.replace("_", " ").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = stageColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = colors.outline.copy(alpha = 0.3f), thickness = 0.8.dp)
            Spacer(Modifier.height(10.dp))

            // Bottom Metrics Row: Facts, Evidence, Timeline, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricBadge(icon = Icons.Outlined.FactCheck, count = matter.facts.size, label = "Facts", colors = colors)
                    MetricBadge(icon = Icons.Outlined.Description, count = matter.evidence.size, label = "Evidence", colors = colors)
                    MetricBadge(icon = Icons.Outlined.Schedule, count = matter.timeline.size, label = "Timeline", colors = colors)
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DomainBadge(domain: String, colors: ColorScheme) {
    val (label, icon) = when {
        domain.contains("tenan", ignoreCase = true) || domain.contains("landlord", ignoreCase = true) ->
            "Tenancy & Eviction" to Icons.Outlined.Home
        domain.contains("cheque", ignoreCase = true) || domain.contains("138", ignoreCase = true) ->
            "Cheque Bounce NI Act" to Icons.Outlined.Payment
        domain.contains("hit", ignoreCase = true) || domain.contains("accident", ignoreCase = true) ->
            "Motor Accident" to Icons.Outlined.DirectionsCar
        domain.contains("police", ignoreCase = true) || domain.contains("fir", ignoreCase = true) ->
            "Police FIR Refusal" to Icons.Outlined.LocalPolice
        domain.contains("cyber", ignoreCase = true) || domain.contains("fraud", ignoreCase = true) ->
            "Cyber Financial Fraud" to Icons.Outlined.Security
        else ->
            domain.ifBlank { "General Legal Matter" } to Icons.Outlined.Gavel
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.primary.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}

@Composable
fun MetricBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, label: String, colors: ColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(3.dp))
        Text(text = "$count $label", fontSize = 11.sp, color = colors.onSurfaceVariant)
    }
}
