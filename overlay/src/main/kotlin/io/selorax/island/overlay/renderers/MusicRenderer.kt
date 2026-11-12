package io.selorax.island.overlay.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.EqualizerBars
import io.selorax.island.designsystem.components.IslandProgressBar
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class MusicRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.MEDIA

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(IslandColors.AccentIon, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", color = Color.White, fontSize = 12.sp)
        }
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity, isRedacted: Boolean) {
        CompactEnd(activity, isRedacted, isPlaying = true)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity, isRedacted: Boolean, isPlaying: Boolean) {
        EqualizerBars(isPlaying = isPlaying)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, isRedacted: Boolean, onAction: (String) -> Unit) {
        val state = activity.state
        val title = when (state) {
            is IslandState.Expanded -> state.title
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "Now Playing"
            else -> "Now Playing"
        }
        val subtitle = when (state) {
            is IslandState.Expanded -> state.subtitle ?: "Media Audio"
            else -> "Media Audio"
        }
        val progress = when (state) {
            is IslandState.Expanded -> (state.contentSlot as? IslandSlot.Progress)?.progress ?: 0.45f
            is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Progress)?.progress ?: 0.45f
            else -> 0.45f
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(IslandColors.AccentIon, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♫", color = Color.White, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1)
                    Text(subtitle, color = IslandColors.OnSurfaceMedium, fontSize = 13.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))
            IslandProgressBar(progress = progress)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("prev") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏮", color = Color.White, fontSize = 20.sp)
                }
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("pause") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏸", color = Color.White, fontSize = 24.sp)
                }
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("next") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏭", color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}
