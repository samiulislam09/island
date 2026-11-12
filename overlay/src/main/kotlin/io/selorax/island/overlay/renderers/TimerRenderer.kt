package io.selorax.island.overlay.renderers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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

public class TimerRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.TIMER

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("⏱", color = IslandColors.AccentIon, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity, isRedacted: Boolean) {
        val state = activity.state
        val label = when (state) {
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "00:00"
            is IslandState.Expanded -> state.title
            else -> "00:00"
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
        val label = when (state) {
            is IslandState.Expanded -> state.title
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "00:00"
            else -> "00:00"
        }
        val progress = when (state) {
            is IslandState.Expanded -> (state.contentSlot as? IslandSlot.Progress)?.progress ?: 0.65f
            is IslandState.Compact -> (state.leadingSlot as? IslandSlot.Progress)?.progress ?: 0.65f
            else -> 0.65f
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(progress = progress, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    color = Color.White,
                    style = IslandTypography.TimerLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("pause") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Pause",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("add1m") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+1 min",
                        color = IslandColors.AccentIon,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("cancel") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cancel",
                        color = IslandColors.Danger,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
