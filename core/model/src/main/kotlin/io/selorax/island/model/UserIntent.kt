package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

@Serializable
public sealed interface UserIntent {
    @Serializable
    public data class Tap(public val activityId: String = "") : UserIntent

    @Serializable
    public data class LongPress(public val activityId: String = "") : UserIntent

    @Serializable
    public data class Swipe(public val direction: Direction) : UserIntent

    @Serializable
    public data class ActionClick(public val activityId: String, public val actionId: String) : UserIntent

    @Serializable
    public data object Collapse : UserIntent

    @Serializable
    public data class Dismiss(public val activityId: String? = null) : UserIntent
}
