package io.selorax.island.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = IslandColors.AccentIon,
    trackColor: Color = Color(0x33FFFFFF)
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val halfStroke = stroke.width / 2f
        val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)

        drawCircle(
            color = trackColor,
            radius = (minOf(this.size.width, this.size.height) - stroke.width) / 2f,
            center = center,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            topLeft = Offset(halfStroke, halfStroke),
            size = arcSize,
            style = stroke
        )
    }
}
