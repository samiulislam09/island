package io.selorax.island.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun IslandProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = IslandColors.AccentIon,
    trackColor: Color = Color(0x33FFFFFF)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}
