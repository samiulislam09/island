package io.selorax.island.model

public data class EffectivePriority(
    public val score: Int,
    public val isUrgent: Boolean = false
) : Comparable<EffectivePriority> {
    override fun compareTo(other: EffectivePriority): Int = score.compareTo(other.score)
}

public enum class IslandPresentation {
    MINIMAL,
    COMPACT,
    SPLIT,
    EXPANDED
}

public enum class IslandVisibility {
    VISIBLE,
    HIDDEN_LANDSCAPE,
    HIDDEN_IMMERSIVE,
    HIDDEN_KEYGUARD,
    HIDDEN_SNOOZED,
    HIDDEN_DISABLED
}
