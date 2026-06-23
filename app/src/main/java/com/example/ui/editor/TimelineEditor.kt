package com.example.ui.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CaptionItem
import com.example.data.model.Project
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.CaptionViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineEditor(
    viewModel: CaptionViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.activeProject.collectAsState()
    val captions by viewModel.activeCaptions.collectAsState()
    val selectedCaption by viewModel.selectedCaption.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var activeSubTab by remember { mutableStateOf("style") } // style, timing, custom, export

    var showExportDialog by remember { mutableStateOf(false) }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No project selected", color = Color.White)
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBack),
        bottomBar = {
            // Elegant back to dashboard and render triggers
            EditorFooterRow(
                onBack = { viewModel.navigateTo("dashboard") },
                onExportClick = { showExportDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Video Preview Canvas Area (Shows focused active subtitle live!)
            VideoPreviewCanvas(
                project = project!!,
                captions = captions,
                selectedCaption = selectedCaption,
                currentTimeMs = currentTimeMs,
                isPlaying = isPlaying,
                onSelectCaption = { viewModel.selectCaption(it) },
                onTogglePlay = { viewModel.togglePlayback() },
                onUpdateCaption = { viewModel.updateSelectedCaption(it) }
            )

            // Dynamic Timeline scrubbing Track Row
            TimelineScrubTrack(
                videoDurationSec = project!!.videoDuration,
                currentTimeMs = currentTimeMs,
                captions = captions,
                selectedCaption = selectedCaption,
                onSeek = { viewModel.seekTo(it) },
                onSelectCaption = { viewModel.selectCaption(it) }
            )

            // Caption details editing text input box (Immediate reflection)
            selectedCaption?.let { activeCap ->
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = activeCap.text,
                            onValueChange = { newT ->
                                viewModel.updateSelectedCaption { it.copy(text = newT) }
                            },
                            label = { Text("Edit focused subtitle text live") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SlateBack,
                                unfocusedContainerColor = SlateBack,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("caption_text_input")
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = { viewModel.deleteSelectedCaption() },
                            modifier = Modifier.background(Color.Red.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete block", tint = Color.Red)
                        }
                    }
                }
            }

            // Quick presets catalog selection bar
            QuickPresetsNavbar(
                activeTab = activeSubTab,
                onTabChange = { activeSubTab = it }
            )

            // Dynamic Control Dashboard Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (activeSubTab) {
                    "style" -> {
                        PresetsCategoryTab(
                            selectedCaption = selectedCaption,
                            onStyleApply = { category, tc, gc, fs, font ->
                                viewModel.updateSelectedCaption {
                                    it.copy(
                                        styleCategory = category,
                                        textColor = tc,
                                        glowColor = gc,
                                        fontSize = fs,
                                        fontFamilyName = font
                                    )
                                }
                            }
                        )
                    }
                    "timing" -> {
                        TimingAdjustmentTab(
                            caption = selectedCaption,
                            onTimeChange = { start, end ->
                                viewModel.updateSelectedCaption {
                                    it.copy(startTimeMs = start, endTimeMs = end)
                                }
                            },
                            onAddNew = { text, s, e ->
                                viewModel.addNewCaption(text, s, e)
                            },
                            currentTimeMs = currentTimeMs
                        )
                    }
                    "custom" -> {
                        CustomAestheticTweaksTab(
                            caption = selectedCaption,
                            onUpdate = { update ->
                                viewModel.updateSelectedCaption(update)
                            }
                        )
                    }
                }
            }
        }

        if (showExportDialog) {
            SuperRenderExportDialog(
                onDismiss = { showExportDialog = false },
                onConfirm = { format, resolution, subtitleBurn ->
                    viewModel.renderProjectVideo(format, resolution, subtitleBurn)
                    showExportDialog = false
                    viewModel.navigateTo("dashboard")
                }
            )
        }
    }
}

@Composable
fun VideoPreviewCanvas(
    project: Project,
    captions: List<CaptionItem>,
    selectedCaption: CaptionItem?,
    currentTimeMs: Long,
    isPlaying: Boolean,
    onSelectCaption: (CaptionItem) -> Unit,
    onTogglePlay: () -> Unit,
    onUpdateCaption: ((CaptionItem) -> CaptionItem) -> Unit
) {
    // Return currently running active caption
    val activeCaption = captions.firstOrNull { currentTimeMs in it.startTimeMs..it.endTimeMs }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp),
        cornerRadius = 14.dp,
        glowColor = PrimaryNeonPink.copy(alpha = 0.2f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Interactive visual grids and subtle video canvas decorative play guides
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStroke = 0.5.dp.toPx()
                val lineCol = Color.White.copy(alpha = 0.05f)
                drawLine(lineCol, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), strokeWidth = gridStroke)
                drawLine(lineCol, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), strokeWidth = gridStroke)
                drawLine(lineCol, Offset(0f, size.height * 0.33f), Offset(size.width, size.height * 0.33f), strokeWidth = gridStroke)
                drawLine(lineCol, Offset(0f, size.height * 0.66f), Offset(size.width, size.height * 0.66f), strokeWidth = gridStroke)
            }

            // Decorative speech wave graphics if playing
            if (isPlaying) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(8) { idx ->
                        val duration = remember { (400..1000).random() }
                        // Draw animating wave height blocks
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(AccentNeonCyan, CircleShape)
                        )
                    }
                }
            }

            // Central Playback controls overlay
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, PrimaryNeonPink.copy(alpha = 0.4f), CircleShape)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Preview Play State",
                    tint = PrimaryNeonPink,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Burning watermark representation if enabled in brand kit
            Text(
                text = "CaptionX Engine Live",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = Color.White.copy(alpha = 0.25f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp
            )

            // Sleek bottom details row matching Sleek Interface design
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave/EQ representation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(16.dp)
                ) {
                    Box(modifier = Modifier.width(2.5.dp).height(10.dp).background(AccentNeonCyan, CircleShape))
                    Box(modifier = Modifier.width(2.5.dp).height(16.dp).background(AccentNeonCyan.copy(alpha = 0.6f), CircleShape))
                    Box(modifier = Modifier.width(2.5.dp).height(8.dp).background(AccentNeonCyan, CircleShape))
                    Box(modifier = Modifier.width(2.5.dp).height(12.dp).background(AccentNeonCyan.copy(alpha = 0.8f), CircleShape))
                }

                // High-fidelity timestamp label matching HTML
                val currentSeconds = currentTimeMs / 1000
                val millisecondsPart = (currentTimeMs % 1000) / 10
                val timestampStr = String.format("%02d:%02d:%02d", currentSeconds / 60, currentSeconds % 60, millisecondsPart)
                Text(
                    text = timestampStr,
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // ACTUAL COMP compositing text overlay layer (Fully interactive drag & resize preview!)
            activeCaption?.let { cap ->
                val textStyle = remember(cap) {
                    TextStyle(
                        color = Color(android.graphics.Color.parseColor(cap.textColor)),
                        fontSize = cap.fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = when (cap.fontFamilyName) {
                            "Monospace" -> FontFamily.Monospace
                            "Serif" -> FontFamily.Serif
                            else -> FontFamily.SansSerif
                        },
                        shadow = if (cap.hasGlow) Shadow(
                            color = Color(android.graphics.Color.parseColor(cap.glowColor)),
                            offset = Offset(0f, 0f),
                            blurRadius = 14f
                        ) else Shadow.None
                    )
                }

                // Interactive wrapper with dragging detection offset and tap to edit select
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                cap.xOffset.roundToInt(),
                                cap.yOffset.roundToInt()
                            )
                        }
                        .align(Alignment.Center)
                        .pointerInput(cap.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onUpdateCaption { current ->
                                    current.copy(
                                        xOffset = (current.xOffset + dragAmount.x).coerceIn(-250f, 250f),
                                        yOffset = (current.yOffset + dragAmount.y).coerceIn(-250f, 250f)
                                    )
                                }
                            }
                        }
                        .clickable { onSelectCaption(cap) }
                        .background(
                            color = if (cap.bgStyle == "Semi-Transparent") Color.Black.copy(alpha = cap.bgOpacity) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (selectedCaption?.id == cap.id) 1.dp else 0.dp,
                            color = if (selectedCaption?.id == cap.id) SecondaryNeonGreen else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cap.text,
                        style = textStyle,
                        modifier = Modifier
                            .scale(cap.scale)
                            .rotate(cap.rotation)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineScrubTrack(
    videoDurationSec: Int,
    currentTimeMs: Long,
    captions: List<CaptionItem>,
    selectedCaption: CaptionItem?,
    onSeek: (Long) -> Unit,
    onSelectCaption: (CaptionItem) -> Unit
) {
    val totalDurationMs = videoDurationSec * 1000L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${"%.1f".format(currentTimeMs / 1000f)}s / ${videoDurationSec}.0s",
                color = AccentNeonCyan,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Scrubber Track",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual track slider track
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SlateBack, RoundedCornerShape(8.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                .pointerInput(totalDurationMs) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val percent = change.position.x / size.width
                        val targetedTime = (percent * totalDurationMs).toLong()
                        onSeek(targetedTime)
                    }
                }
        ) {
            val trackWidth = maxWidth

            // Draw tick background marks on Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickWidth = 1.dp.toPx()
                val tickHeight = 6.dp.toPx()
                val divisions = videoDurationSec * 2
                for (i in 0..divisions) {
                    val x = (size.width / divisions) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(x, size.height - tickHeight),
                        end = Offset(x, size.height),
                        strokeWidth = tickWidth
                    )
                }
            }

            // Subtitle block representations inside track
            captions.forEach { cap ->
                val startPercent = cap.startTimeMs.toFloat() / totalDurationMs
                val endPercent = cap.endTimeMs.toFloat() / totalDurationMs
                val widthPercent = (endPercent - startPercent).coerceIn(0.01f, 1.0f)
                val isSel = selectedCaption?.id == cap.id

                val itemWidth = trackWidth * widthPercent
                val itemOffset = trackWidth * startPercent

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(itemWidth)
                        .offset(x = itemOffset, y = 5.dp)
                        .background(
                            color = if (isSel) SecondaryNeonGreen.copy(alpha = 0.4f) else PrimaryNeonPink.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isSel) SecondaryNeonGreen else PrimaryNeonPink.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectCaption(cap) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cap.text,
                        color = Color.White,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Scrubber Needle position guide line bar representation
            val progressPercent = currentTimeMs.toFloat() / totalDurationMs
            val needleOffset = trackWidth * progressPercent
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = needleOffset, y = 0.dp)
                    .background(AccentNeonCyan)
            )
        }
    }
}

@Composable
fun QuickPresetsNavbar(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(SlateSurface, RoundedCornerShape(10.dp))
            .border(0.5.dp, SlateBorder, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val tabs = listOf(
            PresetTab("style", "Style Preset", Icons.Filled.Style),
            PresetTab("custom", "Aesthetics", Icons.Filled.Palette),
            PresetTab("timing", "Timing", Icons.Filled.Timelapse)
        )

        tabs.forEach { t ->
            val isSel = activeTab == t.id
            IconButton(
                onClick = { onTabChange(t.id) },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSel) PrimaryNeonPink.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = t.icon,
                        contentDescription = t.label,
                        tint = if (isSel) PrimaryNeonPink else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = t.label, color = if (isSel) Color.White else TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

data class PresetTab(val id: String, val label: String, val icon: ImageVector)

@Composable
fun PresetsCategoryTab(
    selectedCaption: CaptionItem?,
    onStyleApply: (String, String, String, Int, String) -> Unit
) {
    val stylePresets = listOf(
        StylePreset("Cyberpunk Neon", "Neon", "#7C3AED", "#2DD4BF", 26, "Monospace"),
        StylePreset("Luxury Gold", "Luxury", "#FFD700", "#FF8C00", 25, "Serif"),
        StylePreset("Classic Subtitle", "Classic", "#FFFFFF", "#000000", 22, "SansSerif"),
        StylePreset("Gaming Green", "Gaming", "#2DD4BF", "#000000", 28, "Monospace"),
        StylePreset("Vlog Aesthetic", "Aesthetic", "#FFFACD", "#FF007F", 24, "Serif"),
        StylePreset("Business Clean", "Business", "#E0ECEF", "#000000", 20, "SansSerif"),
        StylePreset("Podcast Raw", "Podcast", "#FF5722", "#000000", 24, "SansSerif"),
        StylePreset("Futuristic Retro", "Futuristic", "#00FFFF", "#FF00FF", 26, "Monospace")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Style Library".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "1,024 Styles Available",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryNeonPink
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stylePresets) { preset ->
                val isSel = selectedCaption?.styleCategory == preset.category
                
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(110.dp)
                        .background(
                            color = if (isSel) PrimaryNeonPink else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onStyleApply(preset.category, preset.textColor, preset.glowColor, preset.fontSize, preset.fontFamily)
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Aa",
                            fontSize = 24.sp,
                            color = if (isSel) Color.White else Color(android.graphics.Color.parseColor(preset.textColor)),
                            fontWeight = FontWeight.Black,
                            fontFamily = if (preset.fontFamily == "Monospace") FontFamily.Monospace else if (preset.fontFamily == "Serif") FontFamily.Serif else FontFamily.SansSerif,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preset.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class StylePreset(
    val name: String,
    val category: String,
    val textColor: String,
    val glowColor: String,
    val fontSize: Int,
    val fontFamily: String
)

@Composable
fun TimingAdjustmentTab(
    caption: CaptionItem?,
    onTimeChange: (Long, Long) -> Unit,
    onAddNew: (String, Long, Long) -> Unit,
    currentTimeMs: Long
) {
    var newText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (caption != null) {
            Text(text = "Focused Block Boundaries", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = (caption.startTimeMs / 1000f).toString(),
                    onValueChange = { newVal ->
                        val parsed = newVal.toFloatOrNull() ?: 0f
                        onTimeChange((parsed * 1000L).toLong(), caption.endTimeMs)
                    },
                    label = { Text("Start (s)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SlateBack,
                        unfocusedContainerColor = SlateBack
                    ),
                    modifier = Modifier.weight(1f)
                )

                TextField(
                    value = (caption.endTimeMs / 1000f).toString(),
                    onValueChange = { newVal ->
                        val parsed = newVal.toFloatOrNull() ?: 3f
                        onTimeChange(caption.startTimeMs, (parsed * 1000L).toLong())
                    },
                    label = { Text("End (s)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SlateBack,
                        unfocusedContainerColor = SlateBack
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Add custom visual trigger
            Text(text = "Insert custom caption at scrubber head time", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newText,
                    onValueChange = { newText = it },
                    placeholder = { Text("e.g. Look at that! 🤯") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SlateBack,
                        unfocusedContainerColor = SlateBack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_custom_caption_input")
                )

                Spacer(modifier = Modifier.width(10.dp))

                NeonButton(
                    text = "Add Block",
                    onClick = {
                        if (newText.isNotBlank()) {
                            onAddNew(newText, currentTimeMs, currentTimeMs + 2500)
                            newText = ""
                        }
                    },
                    modifier = Modifier,
                    testTag = "add_caption_action"
                )
            }
        }
    }
}

@Composable
fun CustomAestheticTweaksTab(
    caption: CaptionItem?,
    onUpdate: ((CaptionItem) -> CaptionItem) -> Unit
) {
    if (caption == null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Focus a caption in the timeline to unlock meticulous aesthetic sliders.", color = TextMuted)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Precision Aesthetic Customization Sliders", style = MaterialTheme.typography.titleSmall, color = Color.White)

        // Font Size slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Font Size", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text(text = "${caption.fontSize}sp", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            }
            Slider(
                value = caption.fontSize.toFloat(),
                onValueChange = { sizeVal ->
                    onUpdate { it.copy(fontSize = sizeVal.toInt()) }
                },
                valueRange = 12f..50f,
                colors = SliderDefaults.colors(
                    activeTrackColor = PrimaryNeonPink,
                    thumbColor = PrimaryNeonPink
                )
            )
        }

        // Subtitle Scale slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Scale Zoom Ratio", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text(text = "${"%.1f".format(caption.scale)}x", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            }
            Slider(
                value = caption.scale,
                onValueChange = { newVal ->
                    onUpdate { it.copy(scale = newVal) }
                },
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(
                    activeTrackColor = PrimaryNeonPink,
                    thumbColor = PrimaryNeonPink
                )
            )
        }

        // Rotation slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Rotation angle", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text(text = "${caption.rotation.toInt()}°", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            }
            Slider(
                value = caption.rotation,
                onValueChange = { angle ->
                    onUpdate { it.copy(rotation = angle) }
                },
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    activeTrackColor = PrimaryNeonPink,
                    thumbColor = PrimaryNeonPink
                )
            )
        }

        // Glow Color Config
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Neon Glow Overlay", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text(text = "Toggle high-wattage background diffuse glow", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = TextMuted)
            }
            Switch(
                checked = caption.hasGlow,
                onCheckedChange = { toggle ->
                    onUpdate { it.copy(hasGlow = toggle) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryNeonPink,
                    checkedTrackColor = PrimaryNeonPink.copy(alpha = 0.4f)
                )
            )
        }

        // Translucent background card wrapper toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Backplate Translucent Mask", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text(text = "Increases high-key scene contrast readability", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = TextMuted)
            }

            val hasMask = caption.bgStyle == "Semi-Transparent"
            Switch(
                checked = hasMask,
                onCheckedChange = { toggle ->
                    onUpdate { it.copy(bgStyle = if (toggle) "Semi-Transparent" else "None") }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryNeonPink,
                    checkedTrackColor = PrimaryNeonPink.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun EditorFooterRow(
    onBack: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dashboard")
            }
        }

        Button(
            onClick = onExportClick,
            modifier = Modifier.weight(1f).testTag("render_video_action"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonPink)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PhotoCameraBack, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Super-Burn Render")
            }
        }
    }
}

@Composable
fun SuperRenderExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("MP4") }
    var selectedResolution by remember { mutableStateOf("1080P") }
    var subtitleBurn by remember { mutableStateOf(true) }

    val formats = listOf("MP4", "MOV", "WEBM")
    val resolutions = listOf("720P", "1080P", "2K", "4K")

    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            glowColor = PrimaryNeonPink
        ) {
            Text(
                text = "Premium Video Rendering Engine",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "lossless output pipeline with optimal multipass render speeds.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Export format select
            Text(text = "Codec Export Format", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                formats.forEach { form ->
                    val isSel = selectedFormat == form
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSel) PrimaryNeonPink.copy(alpha = 0.15f) else SlateBack,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSel) PrimaryNeonPink else SlateBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedFormat = form }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = form, color = if (isSel) Color.White else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lossless export target resolution select
            Text(text = "Output Lossless Canvas Resolution", style = MaterialTheme.typography.bodySmall, color = AccentNeonCyan)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                resolutions.forEach { res ->
                    val isSel = selectedResolution == res
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSel) PrimaryNeonPink.copy(alpha = 0.15f) else SlateBack,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSel) PrimaryNeonPink else SlateBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedResolution = res }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = res, color = if (isSel) Color.White else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Layer hard burn switch toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Hard-Burn Subtitles permanently", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text(text = "Overlay SRT/VTT/ASS presets seamlessly", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = TextMuted)
                }
                Switch(
                    checked = subtitleBurn,
                    onCheckedChange = { subtitleBurn = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeonPink,
                        checkedTrackColor = PrimaryNeonPink.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Abort")
                }

                Button(
                    onClick = {
                        onConfirm(selectedFormat, selectedResolution, subtitleBurn)
                    },
                    modifier = Modifier.weight(1f).testTag("confirm_export_render_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonPink)
                ) {
                    Text("Trigger Render")
                }
            }
        }
    }
}
