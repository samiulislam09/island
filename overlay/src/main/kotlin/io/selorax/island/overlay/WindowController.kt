package io.selorax.island.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.AbstractComposeView

public class WindowController @JvmOverloads constructor(
    private val windowManager: WindowManager,
    private var view: View? = null,
) {
    private var attachedView: View? = null

    public var isAttached: Boolean = false
        private set

    public var updateCallCount: Int = 0
        private set

    public val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        x = 0
        y = 0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    public fun updatePosition(x: Int, y: Int) {
        layoutParams.x = x
        layoutParams.y = y
        val currentView = attachedView ?: view ?: return
        try {
            windowManager.updateViewLayout(currentView, layoutParams)
            updateCallCount++
        } catch (_: Exception) {}
    }

    @JvmOverloads
    public fun attach(
        x: Int = layoutParams.x,
        y: Int = layoutParams.y,
        width: Int = layoutParams.width,
        height: Int = layoutParams.height,
    ) {
        if (isAttached) return
        layoutParams.x = x
        layoutParams.y = y
        layoutParams.width = width
        layoutParams.height = height

        val target = view ?: attachedView ?: return
        attachedView = target
        try {
            windowManager.addView(target, layoutParams)
            isAttached = true
        } catch (_: Exception) {
            isAttached = false
        }
    }

    public fun detach() {
        if (!isAttached && attachedView == null) return
        val currentView = attachedView ?: view
        if (currentView != null) {
            try {
                windowManager.removeViewImmediate(currentView)
            } catch (_: Exception) {
                // Guard against View not attached or already removed
            }
            try {
                (currentView as? AbstractComposeView)?.disposeComposition()
            } catch (_: Exception) {}
        }
        attachedView = null
        view = null
        isAttached = false
    }

    public fun attachView(view: View) {
        if (isAttached) return
        this.view = view
        attachedView = view
        try {
            windowManager.addView(view, layoutParams)
            isAttached = true
        } catch (_: Exception) {
            isAttached = false
        }
    }

    public fun detachView() {
        detach()
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
        val currentView = attachedView ?: return
        layoutParams.x = bounds.x
        layoutParams.y = bounds.top
        layoutParams.width = if (bounds.isVisible) bounds.width else 0
        layoutParams.height = if (bounds.isVisible) bounds.height else 0

        try {
            windowManager.updateViewLayout(currentView, layoutParams)
            updateCallCount++
        } catch (_: Exception) {}
    }
}
