package io.selorax.island.overlay

import android.view.View
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WindowControllerLifecycleTest {

    private lateinit var windowManager: WindowManager
    private lateinit var view: View
    private lateinit var controller: WindowController

    @Before
    fun setup() {
        windowManager = mockk(relaxed = true)
        view = mockk(relaxed = true)
        controller = WindowController(windowManager, view)
    }

    @Test
    fun attachAndDetach_areIdempotentAndSafe() {
        assertFalse(controller.isAttached)

        controller.attach(0, 0, 100, 40)
        assertTrue(controller.isAttached)

        // Multiple detach calls should not crash or throw
        controller.detach()
        assertFalse(controller.isAttached)
        controller.detach()
        assertFalse(controller.isAttached)
    }

    @Test
    fun attach_isIdempotent() {
        controller.attach(0, 0, 100, 40)
        assertTrue(controller.isAttached)

        // Calling attach again while attached must not call addView again
        controller.attach(0, 0, 100, 40)
        assertTrue(controller.isAttached)

        verify(exactly = 1) {
            windowManager.addView(view, any())
        }
    }

    @Test
    fun detach_handlesWindowManagerExceptionGracefully() {
        controller.attach(0, 0, 100, 40)
        assertTrue(controller.isAttached)

        every { windowManager.removeViewImmediate(any()) } throws IllegalArgumentException("View not attached")

        // Should not throw
        controller.detach()
        assertFalse(controller.isAttached)
    }

    @Test
    fun legacyAttachViewAndDetachView_updateLifecycleCorrectly() {
        val legacyController = WindowController(windowManager)
        assertFalse(legacyController.isAttached)

        legacyController.attachView(view)
        assertTrue(legacyController.isAttached)

        legacyController.detachView()
        assertFalse(legacyController.isAttached)

        // Redundant detachView is safe
        legacyController.detachView()
        assertFalse(legacyController.isAttached)
    }
}
