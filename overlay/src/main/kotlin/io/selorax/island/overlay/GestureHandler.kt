package io.selorax.island.overlay

import io.selorax.island.model.UserIntent
import kotlin.math.abs

public class GestureHandler {
    public fun onDragEnd(
        dragAmountX: Float,
        dragAmountY: Float,
        thresholdPx: Float = 100f
    ): UserIntent? {
        return when {
            dragAmountY < -thresholdPx -> UserIntent.Collapse
            abs(dragAmountX) > thresholdPx -> UserIntent.Dismiss()
            else -> null
        }
    }
}
