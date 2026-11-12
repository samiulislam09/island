package io.selorax.island.overlay

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.unit.IntSize
import io.selorax.island.common.compatibility.ThrottleLevel
import io.selorax.island.model.IslandPresentation
import io.selorax.island.overlay.ui.resolveEffectivePresentation
import io.selorax.island.overlay.ui.resolveMorphAnimationSpec
import io.selorax.island.overlay.ui.shouldRunAnimations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandRendererThrottleTest {

    @Test
    fun equalizerAnimation_throttledUnderLightAndSevere() {
        // Animation should only run when isPlaying is true AND throttleLevel is NORMAL
        val normalRunning = shouldRunAnimations(isPlaying = true, throttle = ThrottleLevel.NORMAL)
        val lightThrottled = shouldRunAnimations(isPlaying = true, throttle = ThrottleLevel.LIGHT_THROTTLE)
        val severeThrottled = shouldRunAnimations(isPlaying = true, throttle = ThrottleLevel.SEVERE_THROTTLE)

        assertTrue(normalRunning)
        assertFalse(lightThrottled)
        assertFalse(severeThrottled)
    }

    @Test
    fun morphAnimationSpec_throttledUsesTween_normalUsesSpring() {
        val normalSpec = resolveMorphAnimationSpec(ThrottleLevel.NORMAL)
        val lightSpec = resolveMorphAnimationSpec(ThrottleLevel.LIGHT_THROTTLE)
        val severeSpec = resolveMorphAnimationSpec(ThrottleLevel.SEVERE_THROTTLE)

        assertTrue(normalSpec is SpringSpec<IntSize>)
        assertTrue(lightSpec is TweenSpec<IntSize>)
        assertTrue(severeSpec is TweenSpec<IntSize>)
    }

    @Test
    fun severeThrottle_autoCollapsesExpandedPresentation() {
        assertEquals(
            IslandPresentation.EXPANDED,
            resolveEffectivePresentation(IslandPresentation.EXPANDED, ThrottleLevel.NORMAL)
        )
        assertEquals(
            IslandPresentation.EXPANDED,
            resolveEffectivePresentation(IslandPresentation.EXPANDED, ThrottleLevel.LIGHT_THROTTLE)
        )
        assertEquals(
            IslandPresentation.COMPACT,
            resolveEffectivePresentation(IslandPresentation.EXPANDED, ThrottleLevel.SEVERE_THROTTLE)
        )
        assertEquals(
            IslandPresentation.SPLIT,
            resolveEffectivePresentation(IslandPresentation.SPLIT, ThrottleLevel.SEVERE_THROTTLE)
        )
        assertEquals(
            IslandPresentation.MINIMAL,
            resolveEffectivePresentation(IslandPresentation.MINIMAL, ThrottleLevel.SEVERE_THROTTLE)
        )
    }
}
