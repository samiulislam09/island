package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public enum class ActivitySource {
    NOTIFICATION,
    MEDIA,
    BATTERY,
    TIMER
}

@Serializable
public enum class ActivityPriority {
    CRITICAL,
    HIGH,
    DEFAULT,
    LOW;

    public val isUrgent: Boolean get() = this == CRITICAL
}

@Serializable
public data class LiveActivity(
    public val key: ActivityKey,
    public val source: EventSource,
    public val priority: ActivityPriority = ActivityPriority.DEFAULT,
    public val state: IslandState,
    public val createdAt: Long,
    public val updatedAt: Long,
    public val visibleSince: Long? = null,
    public val timeoutMs: Long? = null
) {
    public val id: String get() = key.raw
}
