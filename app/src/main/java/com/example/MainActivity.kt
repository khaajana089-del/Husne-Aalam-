package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.editor.TimelineEditor
import com.example.ui.copilot.CopilotPanel
import com.example.ui.theme.AccentNeonCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryNeonPink
import com.example.ui.theme.SlateBack
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.CaptionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CaptionXMainNavigator()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CaptionXMainNavigator() {
    val viewModel: CaptionViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateBack
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "dashboard" -> {
                    DashboardScreen(viewModel = viewModel)
                }
                "editor" -> {
                    StudioEditorView(viewModel = viewModel)
                }
                else -> {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StudioEditorView(viewModel: CaptionViewModel) {
    var editorMode by remember { mutableStateOf("timeline") } // timeline, copilot
    val activeProject by viewModel.activeProject.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBack)
            .statusBarsPadding()
    ) {
        // High-end Studio Editor Header with Mode selectors styled like Sleek Interface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("dashboard") },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Back to dashboard",
                        tint = Color.White
                    )
                }

                // Sleek brand gradient box [X]
                Box(
                    modifier = Modifier
                        .size(28.dp)
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
                        fontSize = 14.sp
                    )
                }

                // Project Title
                Column {
                    Text(
                        text = activeProject?.title ?: "AI Studio",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sleek Engine Live",
                        color = AccentNeonCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Floating Toggle Pill to slide elegantly between Timeline adjustments and our AI Copilot chatbot panels
            Row(
                modifier = Modifier
                    .background(Color.Black, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .padding(2.dp)
            ) {
                IconButton(
                    onClick = { editorMode = "timeline" },
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (editorMode == "timeline") PrimaryNeonPink else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("mode_timeline_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timeline,
                        contentDescription = "Timeline Mode",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = { editorMode = "copilot" },
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (editorMode == "copilot") AccentNeonCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("mode_copilot_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Copilot Mode",
                        tint = if (editorMode == "copilot") Color.Black else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

        // Selected Mode Layout Panel
        Box(modifier = Modifier.weight(1f)) {
            if (editorMode == "timeline") {
                TimelineEditor(viewModel = viewModel)
            } else {
                CopilotPanel(viewModel = viewModel)
            }
        }
    }
}
