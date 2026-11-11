package io.selorax.island.feature.media

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class MediaSessionCollector : EventCollector {
    override val source: EventSource = EventSource.MEDIA

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 32)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun onPlaybackStarted(packageName: String, title: String, artist: String) {
        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("music"),
            trailingSlot = IslandSlot.Text(title.take(12)),
            centerContent = null
        )
        val event = IslandEvent.Upsert(
            key = ActivityKey("media:$packageName"),
            source = EventSource.MEDIA,
            timestamp = System.currentTimeMillis(),
            state = state,
            hints = PriorityHints()
        )
        _events.tryEmit(event)
    }

    public fun onPlaybackStopped(packageName: String) {
        val event = IslandEvent.End(
            key = ActivityKey("media:$packageName"),
            source = EventSource.MEDIA,
            timestamp = System.currentTimeMillis(),
            reason = "playback_paused"
        )
        _events.tryEmit(event)
    }
}
