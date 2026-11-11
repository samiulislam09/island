package io.selorax.island.feature.notifications

import io.selorax.island.common.EventCollector
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class NotificationEventBridge : EventCollector {
    override val source: EventSource = EventSource.NOTIFICATION

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 64)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun emit(event: IslandEvent) {
        _events.tryEmit(event)
    }
}
