package io.selorax.island.overlay.renderers

import androidx.compose.runtime.Composable
import io.selorax.island.model.EventSource
import io.selorax.island.model.LiveActivity

public interface ActivityRenderer {
    public val source: EventSource
    @Composable public fun CompactStart(activity: LiveActivity)
    @Composable public fun CompactEnd(activity: LiveActivity, isRedacted: Boolean = false)
    @Composable public fun CompactEnd(activity: LiveActivity, isRedacted: Boolean, isPlaying: Boolean) {
        CompactEnd(activity, isRedacted)
    }
    @Composable public fun Expanded(activity: LiveActivity, isRedacted: Boolean = false, onAction: (String) -> Unit)
}
