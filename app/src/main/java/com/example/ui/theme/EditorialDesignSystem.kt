package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Editorial design system tokens and modifiers for Pointly 77.
 */
object EditorialDesignSystem {

    // Distinct editorial color palette
    val CreamPaper = Color(0xFFFAF8F5) // Soft creamy off-white
    val DarkSlate = Color(0xFF1E1E24)   // Deep solid slate
    val BrutalistBorder = Color(0xFF000000) // Heavy pitch black border
    val StickyYellow = Color(0xFFFFD54F)   // Classic sticky yellow
    val StickyBlue = Color(0xFF90CAF9)     // Sticky note blue
    val AccentCoral = Color(0xFFFF5252)    // Red/coral margin or sticky pin dot
    
    // Light and Dark theme specific configurations
    @Composable
    fun gridColor(): Color {
        return if (MaterialTheme.colorScheme.isLight()) {
            Color(0xFFE0DCD3).copy(alpha = 0.5f)
        } else {
            Color(0xFF3A3A42).copy(alpha = 0.5f)
        }
    }

    @Composable
    fun marginColor(): Color {
        return if (MaterialTheme.colorScheme.isLight()) {
            Color(0xFFFF8A80).copy(alpha = 0.7f)
        } else {
            Color(0xFFFF5252).copy(alpha = 0.5f)
        }
    }
}

/**
 * Extension to check if current theme is light.
 */
@Composable
fun androidx.compose.material3.ColorScheme.isLight(): Boolean {
    return this.background.luminance() > 0.5f
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/**
 * Custom Modifier to draw a highly stylized notebook grid paper background.
 */
fun Modifier.notebookBackground(
    backgroundColor: Color,
    gridColor: Color,
    marginColor: Color,
    gridSpacing: Dp = 24.dp,
    marginOffset: Dp = 40.dp
): Modifier = this.drawBehind {
    // Fill background solid color
    drawRect(color = backgroundColor)

    val spacingPx = gridSpacing.toPx()
    val width = size.width
    val height = size.height

    // Draw coordinate horizontal grid lines
    var y = 0f
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += spacingPx
    }

    // Draw coordinate vertical grid lines
    var x = 0f
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += spacingPx
    }

    // Left vertical margin line (classic notebook red line)
    val marginXPx = marginOffset.toPx()
    drawLine(
        color = marginColor,
        start = Offset(marginXPx, 0f),
        end = Offset(marginXPx, height),
        strokeWidth = 2f
    )
}

/**
 * Beautiful Brutalist Solid Offset Shadow.
 * Draws a solid filled block behind the element, offset by shadowSize, mimicking heavy editorial cards.
 */
fun Modifier.brutalistShadow(
    shadowSize: Dp = 6.dp,
    shadowColor: Color = Color.Black,
    shapeRadius: Dp = 4.dp
): Modifier = this.drawBehind {
    val sizePx = shadowSize.toPx()
    val radiusPx = shapeRadius.toPx()
    
    // Draw offset drop shadow rectangle with slight rounded corners
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(sizePx, sizePx),
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx)
    )
}

/**
 * A highly reusable modifier for drawing editorial sticky note cards.
 * It combines elevation click anim, border, shadow, and slight rotation.
 */
@Composable
fun Modifier.editorialCard(
    rotation: Float = 0f,
    containerColor: Color = Color.White,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color.Black.copy(alpha = 0.9f),
    shapeRadius: Dp = 4.dp,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 6.dp,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animated scale & rotation adjustment on tap
    val animScale by animateFloatAsState(
        targetValue = if (onClick != null) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val cardModifier = this
        .graphicsLayer {
            rotationZ = rotation
            scaleX = animScale
            scaleY = animScale
        }
        .brutalistShadow(shadowSize = shadowOffset, shadowColor = shadowColor, shapeRadius = shapeRadius)
        .clip(RoundedCornerShape(shapeRadius))
        .background(containerColor)
        .border(borderWidth, borderColor, RoundedCornerShape(shapeRadius))

    return if (onClick != null) {
        cardModifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
    } else {
        cardModifier
    }
}
