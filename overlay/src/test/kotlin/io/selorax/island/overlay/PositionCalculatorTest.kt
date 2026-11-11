package io.selorax.island.overlay

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PositionCalculatorTest {
    private val calculator = PositionCalculator()

    @Test
    fun `center punch hole computes centered pill coordinates`() {
        val input = CutoutSpecs(
            displayWidth = 1080,
            displayHeight = 2400,
            cutoutBoundingRect = Rect(left = 490, top = 20, right = 590, bottom = 120),
            statusBarHeight = 130,
            density = 2.75f,
            isLandscape = false
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isTrue()
        // Pill centered horizontally at displayWidth / 2 = 540
        assertThat(bounds.centerX).isEqualTo(540)
    }

    @Test
    fun `no cutout fallback places pill top center below status bar`() {
        val input = CutoutSpecs(
            displayWidth = 1080,
            displayHeight = 2400,
            cutoutBoundingRect = null,
            statusBarHeight = 80,
            density = 2.5f,
            isLandscape = false
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isTrue()
        assertThat(bounds.centerX).isEqualTo(540)
        assertThat(bounds.top).isAtLeast(input.statusBarHeight)
    }

    @Test
    fun `landscape mode hides the overlay`() {
        val input = CutoutSpecs(
            displayWidth = 2400,
            displayHeight = 1080,
            cutoutBoundingRect = Rect(left = 20, top = 490, right = 120, bottom = 590),
            statusBarHeight = 80,
            density = 2.5f,
            isLandscape = true
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isFalse()
    }
}
