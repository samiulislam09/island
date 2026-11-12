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
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class NotificationRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.NOTIFICATION

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("✉", color = Color.White, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity, isRedacted: Boolean) {
        val state = activity.state
        val label = if (isRedacted) {
            "Notification"
        } else {
            when (state) {
                is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "Message"
                is IslandState.Expanded -> state.title
                else -> "Message"
            }
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, isRedacted: Boolean, onAction: (String) -> Unit) {
        val state = activity.state
        val title = if (isRedacted) {
            "Content Hidden"
        } else {
            when (state) {
                is IslandState.Expanded -> state.title
                is IslandState.Compact -> (state.trailingSlot as? IslandSlot.Text)?.text ?: "New Notification"
                else -> "New Notification"
            }
        }
        val body = if (isRedacted) {
            "Unlock to view message details"
        } else {
            when (state) {
                is IslandState.Expanded -> state.subtitle ?: "Tap to open details in app"
                else -> "Tap to open details in app"
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                color = IslandColors.OnSurfaceMedium,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("dismiss") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss",
                        color = IslandColors.Danger,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onAction("open") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open",
                        color = IslandColors.AccentIon,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
