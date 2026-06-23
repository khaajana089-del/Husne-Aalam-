package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentNeonCyan
import com.example.ui.theme.PrimaryNeonPink
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SurfaceGlass

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    glowColor: Color = PrimaryNeonPink.copy(alpha = 0.15f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f),
                        glowColor.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(
                color = SurfaceGlass,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(clickableModifier)
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = PrimaryNeonPink,
    glow: Boolean = true,
    testTag: String = ""
) {
    val roundedShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .testTag(testTag)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.4f))
                ),
                shape = roundedShape
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        primaryColor.copy(alpha = 0.05f)
                    )
                ),
                shape = roundedShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = TextWhiteGlow(primaryColor),
                modifier = Modifier
            )
        }
    }
}

fun TextWhiteGlow(color: Color): Color {
    // Return high-contrast blend helper
    return Color(0xFFFFFFFF)
}

// Custom Draw Modifier to make text glow beautifully on Canvas
fun Modifier.drawNeonGlow(
    color: Color,
    radius: Float = 15f
): Modifier = this.drawWithContent {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.TRANSPARENT
        setShadowLayer(radius, 0f, 0f, color.hashCode())
    }
    drawContext.canvas.nativeCanvas.drawRect(
        0f, 0f, size.width, size.height, paint
    )
    drawContent()
}
