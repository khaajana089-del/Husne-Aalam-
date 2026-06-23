package com.example.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Project
import com.example.data.model.ExportItem
import com.example.data.model.BrandKit
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.CaptionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: CaptionViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val exportHistory by viewModel.exportHistory.collectAsState()
    val brandKit by viewModel.brandKit.collectAsState()
    val renderingJobs by viewModel.renderingQueue.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf("projects") } // projects, analytics, brandkit, exports

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBack)
    ) {
        // Glowing decorative background canvas spheres
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PrimaryNeonPink.copy(alpha = 0.08f),
                radius = 400f,
                center = Offset(size.width, 200f)
            )
            drawCircle(
                color = AccentNeonCyan.copy(alpha = 0.06f),
                radius = 500f,
                center = Offset(0f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Dashboard Header
            DashboardHeader(onAddNewClick = { showCreateDialog = true })

            // Custom Tab Switcher row
            TabSelectorRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Main Tab Content Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    "projects" -> {
                        ProjectsTab(
                            projects = projects,
                            onProjectSelect = {
                                viewModel.selectProject(it)
                                viewModel.navigateTo("editor")
                            },
                            onProjectDelete = { viewModel.deleteProject(it) }
                        )
                    }
                    "analytics" -> {
                        AnalyticsTab(projects = projects)
                    }
                    "brandkit" -> {
                        BrandKitTab(brandKit = brandKit, onSave = { viewModel.updateBrandKit(it) })
                    }
                    "exports" -> {
                        ExportsTab(exports = exportHistory, renderingJobs = renderingJobs)
                    }
                }
            }
        }

        // FAB to create a project
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp)
                .testTag("add_project_fab"),
            containerColor = PrimaryNeonPink,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Project")
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, duration, lang ->
                    viewModel.createAndOpenProject(title, duration, lang)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardHeader(onAddNewClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gradient 'X' Logo Box
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryNeonPink, AccentNeonCyan)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CaptionX",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = AccentNeonCyan
                    )
                }
                Text(
                    text = "Automated Speech Analyzer",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Live Editor Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable { onAddNewClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = AccentNeonCyan.copy(alpha = dotAlpha),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Live Editor",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TabSelectorRow(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        TabItem("projects", "Projects", Icons.Outlined.Folder),
        TabItem("analytics", "Caption DNA", Icons.Outlined.Analytics),
        TabItem("brandkit", "Brand Kit", Icons.Outlined.Palette),
        TabItem("exports", "Render Queue", Icons.Outlined.CloudUpload)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val isActive = selectedTab == item.id
            val activeColor = if (isActive) PrimaryNeonPink else Color.Transparent
            val textColor = if (isActive) Color.White else TextMuted

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .background(
                        color = if (isActive) SurfaceGlass else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) SlateBorder else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onTabSelected(item.id) }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isActive) AccentNeonCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )
                }
            }
        }
    }
}

data class TabItem(val id: String, val label: String, val icon: ImageVector)

@Composable
fun ProjectsTab(
    projects: List<Project>,
    onProjectSelect: (Project) -> Unit,
    onProjectDelete: (Project) -> Unit
) {
    if (projects.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.MovieCreation,
                contentDescription = null,
                tint = PrimaryNeonPink.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No active projects found",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the plus button below to upload a fresh clip and generate synced captions instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects, key = { it.id }) { proj ->
                ProjectCard(
                    project = proj,
                    onClick = { onProjectSelect(proj) },
                    onDelete = { onProjectDelete(proj) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(project.createdAt))

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}"),
        onClick = onClick,
        glowColor = if (project.viralScore > 90) SecondaryNeonGreen.copy(alpha = 0.15f) else PrimaryNeonPink.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated video thumbnail container with colorful vector overlay
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SlateBack, RoundedCornerShape(12.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Generates dynamic thumbnail waveforms
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val brush = Brush.linearGradient(
                        colors = listOf(PrimaryNeonPink, AccentNeonCyan)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(10f, 60f)
                            quadraticTo(30f, 10f, 40f, 45f)
                            quadraticTo(60f, 75f, 70f, 20f)
                            quadraticTo(90f, 50f, 110f, 35f)
                            quadraticTo(130f, 75f, 150f, 60f)
                        },
                        brush = brush,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Small Video Overlay symbol overlay
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Edit Video",
                        tint = AccentNeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = AccentNeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = project.sourceLanguage,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentNeonCyan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${project.videoDuration}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    // Virality badge
                    Row(
                        modifier = Modifier
                            .background(SecondaryNeonGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, SecondaryNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = SecondaryNeonGreen,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Viral index: ${project.viralScore}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SecondaryNeonGreen,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Project",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnalyticsTab(projects: List<Project>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Metrics Overview Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassmorphicCard(modifier = Modifier.weight(1f)) {
                Text(text = "Total Analyzed Clips", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(
                    text = "${projects.size}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = AccentNeonCyan
                )
                Text(text = "100% cloud rendering accuracy", style = MaterialTheme.typography.bodySmall, color = SecondaryNeonGreen, fontSize = 10.sp)
            }

            GlassmorphicCard(modifier = Modifier.weight(1f)) {
                Text(text = "Avg Viral Score", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                val avg = if (projects.isNotEmpty()) projects.map { it.viralScore }.average().toInt() else 0
                Text(
                    text = "$avg%",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = SecondaryNeonGreen
                )
                Text(text = "+12.4% engagement gain", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan, fontSize = 10.sp)
            }
        }

        // Subtitle Retention DNA Curve Chart
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Caption DNA Retention Velocity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Audience concentration peak points mapped across bilingual segments",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Graph representation drawn strictly on Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Draw grid baselines
                val gridAlpha = 0.08f
                drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f)
                drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 2f)
                drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)

                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(PrimaryNeonPink.copy(alpha = 0.35f), Color.Transparent)
                )

                val path = Path().apply {
                    moveTo(0f, size.height * 0.8f)      // Onboarding frame
                    cubicTo(
                        size.width * 0.25f, size.height * 0.2f, // Dynamic caption popup
                        size.width * 0.4f, size.height * 0.1f,  // Peak retention trigger point
                        size.width * 0.5f, size.height * 0.4f   // Medium segment
                    )
                    cubicTo(
                        size.width * 0.75f, size.height * 0.15f,  // AI Translation popup frame
                        size.width * 0.9f, size.height * 0.55f,   // Final CTA
                        size.width, size.height * 0.25f
                    )
                }

                // Draw filled gradient under path
                val areaPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(areaPath, brush = gradientBrush)

                // Highlight Outline Path
                drawPath(
                    path = path,
                    color = AccentNeonCyan,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Glow point
                drawCircle(PrimaryNeonPink, radius = 6.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.14f))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0s (Intro)", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
                Text(text = "[AI Highlight Point] 🔥", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan, fontSize = 10.sp)
                Text(text = "End Frame", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 10.sp)
            }
        }

        // Feature checklist for future scale
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Autonomous Analytics Insight",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SecondaryNeonGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Auto Emojis increase loop completion rate by 18%", style = MaterialTheme.typography.bodySmall, color = TextWhite)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SecondaryNeonGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Hinglish mixed transcript targets 2x regional audience size", style = MaterialTheme.typography.bodySmall, color = TextWhite)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandKitTab(
    brandKit: BrandKit,
    onSave: (BrandKit) -> Unit
) {
    var primaryColor by remember { mutableStateOf(brandKit.primaryColorHex) }
    var secondaryColor by remember { mutableStateOf(brandKit.secondaryColorHex) }
    var accentColor by remember { mutableStateOf(brandKit.accentColorHex) }
    var watermarkText by remember { mutableStateOf(brandKit.watermarkText) }
    var watermarkEnabled by remember { mutableStateOf(brandKit.watermarkEnabled) }
    var defaultStyle by remember { mutableStateOf(brandKit.defaultStyleCategory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enterprise Brand Kit Configuration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Establish your signature aesthetic to apply automatic branding recommendations instantly across all newly generated projects.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Watermark Signature settings", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text("Watermark Title text") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SlateBack,
                    unfocusedContainerColor = SlateBack,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Embed Watermark permanently", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Switch(
                    checked = watermarkEnabled,
                    onCheckedChange = { watermarkEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeonPink,
                        checkedTrackColor = PrimaryNeonPink.copy(alpha = 0.4f)
                    )
                )
            }
        }

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Brand Theme Palette Colors", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            ColorPickRow(label = "Primary Glow", colorHex = primaryColor, onHexChange = { primaryColor = it })
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickRow(label = "Secondary Shadows", colorHex = secondaryColor, onHexChange = { secondaryColor = it })
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickRow(label = "Accent Highlights", colorHex = accentColor, onHexChange = { accentColor = it })
        }

        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Default Subtitle Engine Preset", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            val styleOptions = listOf("Neon", "Glowing", "Aesthetic", "Minimal", "Cyberpunk", "Podcast")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styleOptions.forEach { opt ->
                    val isSel = defaultStyle == opt
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSel) PrimaryNeonPink.copy(alpha = 0.2f) else SlateBack,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSel) PrimaryNeonPink else SlateBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { defaultStyle = opt }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text(text = opt, color = if (isSel) Color.White else TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        NeonButton(
            text = "Save Brand Configuration",
            onClick = {
                onSave(
                    BrandKit(
                        primaryColorHex = primaryColor,
                        secondaryColorHex = secondaryColor,
                        accentColorHex = accentColor,
                        watermarkText = watermarkText,
                        watermarkEnabled = watermarkEnabled,
                        defaultStyleCategory = defaultStyle
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ColorPickRow(
    label: String,
    colorHex: String,
    onHexChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextWhite)
            Text(text = colorHex, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Dynamic small color visual box
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(android.graphics.Color.parseColor(colorHex)), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Quick preset selectors
            val colorsList = listOf("#FF007F", "#39FF14", "#00F0FF", "#FFB600", "#9B51E0")
            colorsList.forEach { col ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(18.dp)
                        .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                        .clickable { onHexChange(col) }
                )
            }
        }
    }
}

@Composable
fun ExportsTab(
    exports: List<ExportItem>,
    renderingJobs: List<com.example.ui.viewmodel.RenderJob>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active rendering tasks row
        if (renderingJobs.isNotEmpty()) {
            item {
                Text(
                    text = "Active Video Rendering Queue",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentNeonCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(renderingJobs) { job ->
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = PrimaryNeonPink.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = job.projectTitle, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(text = "${job.format} • ${job.resolution}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }

                        // Status Badge
                        val isComp = job.status == "Completed"
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isComp) SecondaryNeonGreen.copy(alpha = 0.15f) else PrimaryNeonPink.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = if (isComp) SecondaryNeonGreen else PrimaryNeonPink,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isComp) "COMPLETED" else "RENDERING",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isComp) SecondaryNeonGreen else PrimaryNeonPink,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { job.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = PrimaryNeonPink,
                            trackColor = SlateBack
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${(job.progress * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Export Logs Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Export Logs History (No Watermark Lossless)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (exports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No final files exported yet. Open any project and click Export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(exports) { exp ->
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = exp.projectTitle, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                text = "${exp.resolution} • ${exp.format} • ${"%.1f".format(exp.fileSizeMb)} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        // Success Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SecondaryNeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Lossless", style = MaterialTheme.typography.bodySmall, color = SecondaryNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("15") }
    var selectedLanguage by remember { mutableStateOf("Hinglish / Mixed") }

    val languages = listOf("Hinglish / Mixed", "English", "Hindi (हिन्दी)", "Spanish", "French")

    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            glowColor = PrimaryNeonPink
        ) {
            Text(
                text = "Autonomous AI Caption Studio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Analyze speech, visual cues, emotions, and audience contrast instantly before burning caption layers.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Project Title Name") },
                placeholder = { Text("e.g. Daily Vlog 04") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SlateBack,
                    unfocusedContainerColor = SlateBack,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("project_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("Video Duration (Seconds)") },
                placeholder = { Text("e.g. 15") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SlateBack,
                    unfocusedContainerColor = SlateBack,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("project_duration_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Conversational Native Language", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .verticalScroll(rememberScrollState())
                    .background(SlateBack, RoundedCornerShape(8.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            ) {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = lang }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = lang, color = if (selectedLanguage == lang) Color.White else TextMuted, fontSize = 13.sp)
                        if (selectedLanguage == lang) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = AccentNeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val finalTitle = if (title.isBlank()) "CaptionX Project" else title
                        val duration = durationText.toIntOrNull() ?: 15
                        onConfirm(finalTitle, duration, selectedLanguage)
                    },
                    modifier = Modifier.weight(1f).testTag("confirm_create_project_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonPink)
                ) {
                    Text("Analyze & Create")
                }
            }
        }
    }
}
