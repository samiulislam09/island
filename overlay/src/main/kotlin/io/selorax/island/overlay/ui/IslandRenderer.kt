package io.selorax.island.overlay.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.engine.LiveActivityManager
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.UserIntent

@Composable
public fun IslandRenderer(
    state: LiveActivityManager.InternalIslandState,
    onIntent: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = state.primary ?: return

    Box(
        modifier = modifier.clickable { onIntent(UserIntent.Tap()) },
        contentAlignment = Alignment.Center
    ) {
        when (state.presentation) {
            IslandPresentation.COMPACT -> RenderCompact(primary.state)
            IslandPresentation.EXPANDED -> RenderExpanded(primary.state)
            IslandPresentation.SPLIT -> RenderSplit(primary.state, state.secondary?.state)
            IslandPresentation.MINIMAL -> RenderCompact(primary.state)
        }
    }
}

@Composable
private fun RenderCompact(state: IslandState) {
    if (state is IslandState.Compact) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderSlot(state.leadingSlot)
            Spacer(modifier = Modifier.width(8.dp))
            RenderSlot(state.trailingSlot)
        }
    }
}

@Composable
private fun RenderExpanded(state: IslandState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (state is IslandState.Compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RenderSlot(state.leadingSlot)
                Spacer(modifier = Modifier.width(12.dp))
                RenderSlot(state.trailingSlot)
            }
        }
    }
}

@Composable
private fun RenderSplit(primary: IslandState, secondary: IslandState?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RenderCompact(primary)
        if (secondary != null) {
            Spacer(modifier = Modifier.width(8.dp))
            RenderCompact(secondary)
        }
    }
}

@Composable
private fun RenderSlot(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(text = slot.text, color = Color.White, fontSize = 12.sp)
        is IslandSlot.Icon -> Text(text = "●", color = Color.White, fontSize = 12.sp)
        is IslandSlot.Progress -> Text(text = "${(slot.progress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}
