package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public enum class EventSource {
    NOTIFICATION,
    MEDIA,
    BATTERY,
    TIMER,
    SYSTEM
}

@Serializable
public data class ActivityKey(public val raw: String)

@Serializable
public data class PriorityHints(
    public val priority: ActivityPriority = ActivityPriority.DEFAULT,
    public val isUrgent: Boolean = false
)

@Serializable
public sealed interface IslandEvent {
    public val key: ActivityKey
    public val source: EventSource
    public val timestamp: Long

    @Serializable
    public data class Upsert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val hints: PriorityHints = PriorityHints()
    ) : IslandEvent

    @Serializable
    public data class End(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val reason: String? = null
    ) : IslandEvent

    @Serializable
    public data class Alert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val durationMs: Long
    ) : IslandEvent

    @Serializable
    public data class Batch(
        public val events: List<IslandEvent>,
        override val key: ActivityKey = ActivityKey("batch:events"),
        override val source: EventSource = EventSource.SYSTEM,
        override val timestamp: Long = System.currentTimeMillis()
    ) : IslandEvent
}
