package io.selorax.island.overlay

import io.selorax.island.common.compatibility.CutoutType
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptivePositionCalculatorTest {

    @Test
    fun calculateAdaptivePosition_centerPunch_centeredHorizontally() {
        val pos = PositionCalculator.calculateAdaptivePosition(
            screenWidth = 1080,
            pillWidth = 400,
            statusBarHeight = 80,
            cutoutType = CutoutType.CENTER_PUNCH
        )
        // Centered: (1080 - 400) / 2 = 340
        assertEquals(340, pos.x)
        assertEquals(80, pos.y)
    }

    @Test
    fun calculateAdaptivePosition_cornerLeft_hugsLeftCutout() {
        val pos = PositionCalculator.calculateAdaptivePosition(
            screenWidth = 1080,
            pillWidth = 400,
            statusBarHeight = 80,
            cutoutType = CutoutType.CORNER_PUNCH_LEFT,
            cutoutBounds = android.graphics.Rect(60, 0, 120, 60)
        )
        // Hugs right edge of left cutout + 12px padding: 120 + 12 = 132
        assertEquals(132, pos.x)
    }

    @Test
    fun calculateAdaptivePosition_cornerRight_hugsRightCutout() {
        val pos = PositionCalculator.calculateAdaptivePosition(
            screenWidth = 1080,
            pillWidth = 400,
            statusBarHeight = 80,
            cutoutType = CutoutType.CORNER_PUNCH_RIGHT,
            cutoutBounds = android.graphics.Rect(960, 0, 1020, 60)
        )
        // Left edge of right cutout - pillWidth - 12px padding: 960 - 400 - 12 = 548
        assertEquals(548, pos.x)
    }

    @Test
    fun calculateAdaptivePosition_foldableScreenChange_recomputesCenter() {
        // Outer screen (folded: 900px wide)
        val outerPos = PositionCalculator.calculateAdaptivePosition(
            screenWidth = 900,
            pillWidth = 400,
            statusBarHeight = 80,
            cutoutType = CutoutType.CENTER_PUNCH
        )
        assertEquals(250, outerPos.x)

        // Inner screen (unfolded: 1800px wide)
        val innerPos = PositionCalculator.calculateAdaptivePosition(
            screenWidth = 1800,
            pillWidth = 400,
            statusBarHeight = 80,
            cutoutType = CutoutType.CENTER_PUNCH
        )
        assertEquals(700, innerPos.x)
    }
}
