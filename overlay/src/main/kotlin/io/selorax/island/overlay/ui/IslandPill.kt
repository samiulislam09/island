package io.selorax.island.overlay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState

@Composable
public fun IslandPill(
    state: IslandState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    Box(
        modifier = modifier
            .wrapContentHeight()
            .background(Color.Black, shape = RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .animateContentSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is IslandState.Compact -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SlotContent(slot = state.leadingSlot)
                    SlotContent(slot = state.trailingSlot)
                }
            }
            is IslandState.Expanded -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = state.title, color = Color.White, fontSize = 16.sp)
                    state.subtitle?.let {
                        Text(text = it, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun SlotContent(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(text = slot.text, color = Color.White, fontSize = 12.sp)
        is IslandSlot.Icon -> Text(text = "●", color = Color.Green, fontSize = 12.sp)
        is IslandSlot.Progress -> Text(text = "${(slot.progress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}
