package io.selorax.island.model

public enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    WARNING
}

public sealed interface EngineEffect {
    public data class TriggerHaptic(public val effectType: HapticType) : EngineEffect
    public data class LaunchIntent(public val actionId: String, public val activityKey: ActivityKey) : EngineEffect
}
