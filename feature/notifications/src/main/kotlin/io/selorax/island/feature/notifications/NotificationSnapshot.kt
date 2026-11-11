package io.selorax.island.feature.notifications

public data class NotificationSnapshot(
    public val key: String,
    public val packageName: String,
    public val postTime: Long,
    public val title: String?,
    public val text: String?,
    public val isOngoing: Boolean,
    public val isGroupSummary: Boolean,
    public val category: String?
)
