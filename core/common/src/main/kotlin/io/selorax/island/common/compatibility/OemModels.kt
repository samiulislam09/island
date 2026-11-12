package io.selorax.island.common.compatibility

public enum class OemType {
    SAMSUNG,
    XIAOMI,
    OPPO_ONEPLUS,
    VIVO,
    MOTOROLA,
    STOCK_OR_OTHER
}

public enum class CutoutType {
    CENTER_PUNCH,
    CORNER_PUNCH_LEFT,
    CORNER_PUNCH_RIGHT,
    TEARDROP_NOTCH,
    PILL_DUAL,
    UNDER_DISPLAY_OR_NONE
}

public data class OemRestrictionProfile(
    public val oemType: OemType,
    public val title: String,
    public val description: String,
    public val instructions: List<String>,
    public val settingsIntentAction: String? = null,
    public val settingsPackage: String? = null,
    public val settingsComponent: String? = null
)

public data class DeviceCapabilities(
    public val oemType: OemType,
    public val cutoutType: CutoutType,
    public val isFoldable: Boolean,
    public val oemProfile: OemRestrictionProfile?
)
