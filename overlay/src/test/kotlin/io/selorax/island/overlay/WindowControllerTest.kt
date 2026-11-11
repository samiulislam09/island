package io.selorax.island.overlay

import android.view.View
import android.view.WindowManager
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WindowControllerTest {
    private val windowManager = mockk<WindowManager>(relaxed = true)
    private val targetView = mockk<View>(relaxed = true)

    @Test
    fun `attachView invokes addView with controller layoutParams`() {
        val controller = WindowController(windowManager)
        controller.attachView(targetView)

        verify(exactly = 1) {
            windowManager.addView(targetView, controller.layoutParams)
        }
    }

    @Test
    fun `transition update coalesces to at most 2 layout updates`() {
        val controller = WindowController(windowManager)
        controller.attachView(targetView)

        // Begin transition to expanded
        controller.onTransitionStart(isExpanded = true, bounds = PillBounds(0, 0, 800, 400, true))
        // Multiple intermediate ticks during animation
        controller.onAnimationTick()
        controller.onAnimationTick()
        // Settle transition
        controller.onTransitionEnd(bounds = PillBounds(0, 0, 800, 400, true))

        // Total updateViewLayout calls must be exactly 2 (start and end), ticks ignored
        assertThat(controller.updateCallCount).isEqualTo(2)
    }
}
