package io.selorax.island.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = IslandColors.AccentIon
) {
    if (isPlaying) {
        AnimatedEqualizerBars(modifier = modifier, barColor = barColor)
    } else {
        StaticEqualizerBars(modifier = modifier, barColor = barColor)
    }
}

@Composable
private fun StaticEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = IslandColors.AccentIon
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(4.dp).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(8.dp).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(4.dp).background(barColor, RoundedCornerShape(1.5.dp)))
    }
}

@Composable
private fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = IslandColors.AccentIon
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(h1.dp).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(h2.dp).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(h3.dp).background(barColor, RoundedCornerShape(1.5.dp)))
    }
}
