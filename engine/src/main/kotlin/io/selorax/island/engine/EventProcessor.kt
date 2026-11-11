package io.selorax.island.engine

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge

public class EventProcessor(
    private val collectors: Set<EventCollector>,
    private val isNotificationAllowed: StateFlow<Boolean>,
    private val dedupeWindowMs: Long = 500L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val lastSeen = mutableMapOf<ActivityKey, Long>()

    public fun process(): Flow<IslandEvent> {
        val allFlows = collectors.map { it.events }
        return allFlows.merge()
            .filter { event ->
                if (event.source == EventSource.NOTIFICATION && !isNotificationAllowed.value) {
                    return@filter false
                }
                val now = clock()
                val last = lastSeen[event.key]
                if (last != null && (now - last) < dedupeWindowMs) {
                    false
                } else {
                    lastSeen[event.key] = now
                    true
                }
            }
    }
}
