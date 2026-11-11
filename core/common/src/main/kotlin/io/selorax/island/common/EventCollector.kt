package io.selorax.island.common

import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow

public interface EventCollector {
    public val source: EventSource
    public val events: Flow<IslandEvent>
}
