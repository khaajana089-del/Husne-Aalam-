package com.example.ui.copilot

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CaptionViewModel
import com.example.ui.viewmodel.CopilotMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CopilotPanel(
    viewModel: CaptionViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.copilotMessages.collectAsState()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsState()

    var inputCommand by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBack)
            .padding(16.dp)
    ) {
        // AI Copilot Panel Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = AccentNeonCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CaptionX AI Copilot",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        Text(
            text = "Active multi-language speech analyzer & design assistant.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick AI copilot actions workflow enhancers grid
        Text(text = "Automated Creator Quick Optimizers", style = MaterialTheme.typography.bodySmall, color = TextWhite)
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CopilotQuickActionChips(
                label = "Auto Colors",
                icon = Icons.Filled.ColorLens,
                onClick = { viewModel.triggerCopilotAction("auto_color") },
                testTag = "copilot_auto_color"
            )
            CopilotQuickActionChips(
                label = "Analyze Speech",
                icon = Icons.Filled.EmojiEmotions,
                onClick = { viewModel.triggerCopilotAction("auto_emoji") },
                testTag = "copilot_auto_emoji"
            )
            CopilotQuickActionChips(
                label = "Optimize Style",
                icon = Icons.Filled.Style,
                onClick = { viewModel.triggerCopilotAction("preset_style") },
                testTag = "copilot_preset_style"
            )
            CopilotQuickActionChips(
                label = "Viral Audit",
                icon = Icons.Filled.Timeline,
                onClick = { viewModel.triggerCopilotAction("engagement") },
                testTag = "copilot_engagement"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sleek Interface Recommendation Design Card
        SleekCopilotSuggestionCard(
            onRefineClick = {
                viewModel.submitCopilotCommand("Refine suggestions for viral TikTok visual aesthetics.")
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chat Log List containing conversations
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SlateSurface, RoundedCornerShape(12.dp))
                .border(0.5.dp, SlateBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true, // keeps the latest at the bottom
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reversing for easy natural reading flow
                items(messages.reversed()) { msg ->
                    BubbleMessageCard(msg)
                }

                if (isCopilotLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryNeonPink,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "CaptionX Engine analyzing...", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Command Sender TextInput
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = { Text("Ask Copilot (e.g. recommend style for Hinglish)") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SlateBack,
                    unfocusedContainerColor = SlateBack,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("copilot_input_chat")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.submitCopilotCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                modifier = Modifier
                    .background(PrimaryNeonPink, CircleShape)
                    .testTag("copilot_send_message")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Query Copilot", tint = Color.White)
            }
        }
    }
}

@Composable
fun CopilotQuickActionChips(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Box(
        modifier = Modifier
            .background(SlateSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentNeonCyan, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BubbleMessageCard(msg: CopilotMessage) {
    val isUser = msg.sender == "user"
    val isSystem = msg.sender == "system"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = when {
        isUser -> PrimaryNeonPink.copy(alpha = 0.15f)
        isSystem -> SlateBack
        else -> SlateSurface
    }
    val borderColor = when {
        isUser -> PrimaryNeonPink
        isSystem -> Color.Transparent
        else -> AccentNeonCyan.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
        } else {
            RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
        }

        Box(
            modifier = Modifier
                .background(bgColor, bubbleShape)
                .border(0.5.dp, borderColor, bubbleShape)
                .padding(10.dp)
                .widthIn(max = 240.dp)
        ) {
            Text(
                text = msg.content,
                fontSize = 12.sp,
                color = if (isUser) Color.White else TextWhite,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
        }
        Text(
            text = if (isUser) "You" else if (isSystem) "System Notify" else "CaptionX AI",
            fontSize = 9.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SleekCopilotSuggestionCard(
    modifier: Modifier = Modifier,
    onRefineClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI COPILOT",
                        color = PrimaryNeonPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Viral Score: ",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "98.4",
                            color = AccentNeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .clickable { onRefineClick() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Refine Suggestions",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Analyze tone: High energy, inspirational. Suggesting Futuristic Neon style with mid-screen placement for TikTok engagement.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 15.sp
            )
        }
    }
}
