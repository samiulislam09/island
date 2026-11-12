package io.selorax.island.designsystem

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

public object IslandMotion {
    public const val morphDamping: Float = 0.82f
    public const val morphStiffness: Float = 380f

    public const val contentDamping: Float = 1.0f
    public const val contentStiffness: Float = 600f

    public const val alertDamping: Float = 0.55f
    public const val alertStiffness: Float = 300f

    public fun <T> morphSpring(): SpringSpec<T> = spring(dampingRatio = morphDamping, stiffness = morphStiffness)
    public fun <T> contentSpring(): SpringSpec<T> = spring(dampingRatio = contentDamping, stiffness = contentStiffness)
    public fun <T> alertSpring(): SpringSpec<T> = spring(dampingRatio = alertDamping, stiffness = alertStiffness)
}
