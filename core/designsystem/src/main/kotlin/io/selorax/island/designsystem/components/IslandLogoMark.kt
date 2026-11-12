package io.selorax.island.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium brand logo mark for Dynamic Island:
 * - Concentric glowing orbital field with dynamic breathing pulse
 * - Precision obsidian hardware capsule pill
 * - Optical punch hole lens with anti-reflective glass specular highlight
 * - Real-time activity pulse frequency bars
 */
@Composable
public fun IslandLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    accentColor: Color = Color(0xFF00E5FF),
    isAnimated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "IslandLogoTransition")

    val pulseAlpha by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LogoPulseAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.6f) }
    }

    val orbitRotation by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "LogoOrbitRotation"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val barPulse by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LogoBarPulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.7f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)

            // 1. Ambient Radial Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = pulseAlpha * 0.40f),
                        accentColor.copy(alpha = pulseAlpha * 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = center
            )

            // 2. Futuristic Orbital Ellipse Halo (Tilted)
            rotate(degrees = -18f + (if (isAnimated) orbitRotation * 0.15f else 0f), pivot = center) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.9f),
                            Color(0xFF7C4DFF).copy(alpha = 0.8f),
                            accentColor.copy(alpha = 0.15f),
                            Color(0xFF00E676).copy(alpha = 0.7f),
                            accentColor.copy(alpha = 0.9f)
                        ),
                        center = center
                    ),
                    topLeft = Offset(w * 0.08f, h * 0.28f),
                    size = Size(w * 0.84f, h * 0.44f),
                    style = Stroke(width = w * 0.026f)
                )
            }

            // 3. Floating Obsidian Island Capsule Pill
            val pillWidth = w * 0.68f
            val pillHeight = h * 0.34f
            val pillTopLeft = Offset((w - pillWidth) / 2f, (h - pillHeight) / 2f)
            val pillRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)

            // Deep Obsidian Background
            drawRoundRect(
                color = Color(0xFF07070A),
                topLeft = pillTopLeft,
                size = Size(pillWidth, pillHeight),
                cornerRadius = pillRadius
            )

            // Machined Bezel Rim Stroke
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        accentColor.copy(alpha = 0.70f),
                        Color(0xFF222228),
                        Color(0xFF141418)
                    ),
                    start = pillTopLeft,
                    end = Offset(pillTopLeft.x + pillWidth, pillTopLeft.y + pillHeight)
                ),
                topLeft = pillTopLeft,
                size = Size(pillWidth, pillHeight),
                cornerRadius = pillRadius,
                style = Stroke(width = w * 0.022f)
            )

            // 4. Punch Hole Optical Camera Lens (Left side)
            val lensCenterX = pillTopLeft.x + (pillHeight / 2f) + (pillWidth * 0.06f)
            val lensCenterY = center.y
            val lensRadius = pillHeight * 0.28f

            // Lens outer ring halo
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = lensRadius + (w * 0.015f),
                center = Offset(lensCenterX, lensCenterY),
                style = Stroke(width = w * 0.018f)
            )

            // Lens optical core
            drawCircle(
                color = Color(0xFF000000),
                radius = lensRadius,
                center = Offset(lensCenterX, lensCenterY)
            )

            // Anti-reflective specular glass reflection (micro crescent / dot)
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = lensRadius * 0.32f,
                center = Offset(lensCenterX - (lensRadius * 0.35f), lensCenterY - (lensRadius * 0.35f))
            )

            // 5. Dynamic Activity Equalizer / Signal Beams (Right side)
            val rightContentX = pillTopLeft.x + pillWidth - (pillHeight / 2f) - (pillWidth * 0.08f)
            val barSpacing = w * 0.038f
            val barWidth = w * 0.025f

            // Bar 1 (Left)
            val h1 = pillHeight * (0.35f + 0.30f * barPulse)
            drawLine(
                color = accentColor,
                start = Offset(rightContentX - barSpacing, center.y - (h1 / 2f)),
                end = Offset(rightContentX - barSpacing, center.y + (h1 / 2f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )

            // Bar 2 (Middle)
            val h2 = pillHeight * (0.65f - 0.25f * barPulse)
            drawLine(
                color = Color.White.copy(alpha = 0.95f),
                start = Offset(rightContentX, center.y - (h2 / 2f)),
                end = Offset(rightContentX, center.y + (h2 / 2f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )

            // Bar 3 (Right)
            val h3 = pillHeight * (0.28f + 0.38f * (1f - barPulse))
            drawLine(
                color = accentColor,
                start = Offset(rightContentX + barSpacing, center.y - (h3 / 2f)),
                end = Offset(rightContentX + barSpacing, center.y + (h3 / 2f)),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
