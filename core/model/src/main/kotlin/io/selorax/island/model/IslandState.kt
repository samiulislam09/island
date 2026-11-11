package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public sealed interface IslandSlot {
    @Serializable
    public data class Icon(public val iconName: String) : IslandSlot

    @Serializable
    public data class Text(public val text: String) : IslandSlot

    @Serializable
    public data class Progress(public val progress: Float) : IslandSlot
}

@Serializable
public sealed interface IslandState {
    public val isVisible: Boolean

    @Serializable
    public data object Hidden : IslandState {
        override val isVisible: Boolean = false
    }

    @Serializable
    public data class Compact(
        public val leadingSlot: IslandSlot,
        public val trailingSlot: IslandSlot,
        public val centerContent: IslandSlot? = null
    ) : IslandState {
        override val isVisible: Boolean = true
    }

    @Serializable
    public data class Expanded(
        public val title: String,
        public val subtitle: String?,
        public val leadingSlot: IslandSlot?,
        public val contentSlot: IslandSlot?,
        public val actions: List<IslandAction> = emptyList()
    ) : IslandState {
        override val isVisible: Boolean = true
    }

    @Serializable
    public data class Split(
        public val primary: IslandSlot,
        public val secondary: IslandSlot
    ) : IslandState {
        override val isVisible: Boolean = true
    }
}

@Serializable
public data class IslandAction(
    public val id: String,
    public val label: String,
    public val iconName: String? = null
)
