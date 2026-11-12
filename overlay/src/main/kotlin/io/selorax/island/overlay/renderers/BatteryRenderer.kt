package io.selorax.island.overlay.renderers

import androidx.compose.foundation.layout.Column
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
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.IslandTypography
import io.selorax.island.designsystem.components.RingProgress
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class BatteryRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.BATTERY

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("⚡", color = IslandColors.Success, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity, isRedacted: Boolean) {
        val state = activity.state
        val label = when (state) {
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "Battery"
            is IslandState.Expanded -> state.title
            else -> "Battery"
        }
        Text(
            text = label,
            color = Color.White,
            style = IslandTypography.TimerMedium
        )
    }

    @Composable
    override fun Expanded(activity: LiveActivity, isRedacted: Boolean, onAction: (String) -> Unit) {
        val state = activity.state
        val title = when (state) {
            is IslandState.Expanded -> state.title
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "Battery Status"
            else -> "Battery Status"
        }
        val subtitle = when (state) {
            is IslandState.Expanded -> state.subtitle ?: "Charging connected"
            else -> "Charging connected"
        }
        val progress = when (state) {
            is IslandState.Expanded -> (state.contentSlot as? IslandSlot.Progress)?.progress ?: 0.85f
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Progress)?.progress
                ?: (state.leadingSlot as? IslandSlot.Progress)?.progress ?: 0.85f
            else -> 0.85f
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingProgress(progress = progress, size = 36.dp, color = IslandColors.Success)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, style = IslandTypography.TimerMedium.copy(fontSize = 16.sp))
                Text(subtitle, color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
            }
        }
    }
}
