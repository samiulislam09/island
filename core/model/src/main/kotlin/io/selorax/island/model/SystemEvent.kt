package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public sealed interface SystemEvent {
    @Serializable
    public data class ScreenState(public val isInteractive: Boolean, public val isLocked: Boolean) : SystemEvent

    @Serializable
    public data class Orientation(public val isLandscape: Boolean) : SystemEvent

    @Serializable
    public data class PermissionsChanged(public val hasOverlay: Boolean, public val hasNotificationAccess: Boolean) : SystemEvent
}
