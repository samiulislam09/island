package io.selorax.island.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

public class WindowController(
    private val windowManager: WindowManager
) {
    private var attachedView: View? = null
    public var updateCallCount: Int = 0
        private set

    public val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 0
    }

    public fun attachView(view: View) {
        if (attachedView == null) {
            attachedView = view
            try {
                windowManager.addView(view, layoutParams)
            } catch (_: Exception) {}
        }
    }

    public fun detachView() {
        attachedView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: Exception) {}
            attachedView = null
        }
    }

    public fun onTransitionStart(isExpanded: Boolean, bounds: PillBounds) {
        applyBounds(bounds)
    }

    public fun onAnimationTick() {
        // ADR-03: No IPC window resize calls during intermediate animation frames
    }

    public fun onTransitionEnd(bounds: PillBounds) {
        applyBounds(bounds)
    }

    private fun applyBounds(bounds: PillBounds) {
        val view = attachedView ?: return
        layoutParams.x = bounds.x
        layoutParams.y = bounds.top
        layoutParams.width = if (bounds.isVisible) bounds.width else 0
        layoutParams.height = if (bounds.isVisible) bounds.height else 0

        try {
            windowManager.updateViewLayout(view, layoutParams)
            updateCallCount++
        } catch (_: Exception) {}
    }
}
