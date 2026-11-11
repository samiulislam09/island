package io.selorax.island.data.settings

import kotlinx.serialization.Serializable

@Serializable
public data class IslandSettings(
    public val schemaVersion: Int = 1,
    public val islandEnabled: Boolean = true,
    public val anchor: String = "CUTOUT",
    public val compactWidthDp: Float = 160f,
    public val compactHeightDp: Float = 38f,
    public val hideInLandscape: Boolean = true,
    public val onboardingCompleted: Boolean = false,
    public val notificationDisclosureAccepted: Boolean = false
)
