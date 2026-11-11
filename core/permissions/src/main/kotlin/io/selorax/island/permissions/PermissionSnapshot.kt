package io.selorax.island.permissions

public data class PermissionSnapshot(
    public val hasOverlayPermission: Boolean,
    public val hasNotificationAccess: Boolean,
    public val canScheduleExactAlarms: Boolean
) {
    public val isFullyOperational: Boolean
        get() = hasOverlayPermission && hasNotificationAccess
}
