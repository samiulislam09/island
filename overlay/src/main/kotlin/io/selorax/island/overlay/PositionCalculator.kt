package io.selorax.island.overlay

public data class Rect(public val left: Int, public val top: Int, public val right: Int, public val bottom: Int) {
    public val width: Int get() = right - left
    public val height: Int get() = bottom - top
    public val centerX: Int get() = left + width / 2
}

public data class CutoutSpecs(
    public val displayWidth: Int,
    public val displayHeight: Int,
    public val cutoutBoundingRect: Rect?,
    public val statusBarHeight: Int,
    public val density: Float,
    public val isLandscape: Boolean
)

public data class PillBounds(
    public val x: Int,
    public val top: Int,
    public val width: Int,
    public val height: Int,
    public val isVisible: Boolean
) {
    public val centerX: Int get() = x + width / 2
}

public class PositionCalculator {
    public fun calculate(specs: CutoutSpecs, isExpanded: Boolean): PillBounds {
        if (specs.isLandscape) {
            return PillBounds(x = 0, top = 0, width = 0, height = 0, isVisible = false)
        }

        val baseCompactWidth = (160 * specs.density).toInt()
        val baseCompactHeight = (38 * specs.density).toInt()
        val baseExpandedWidth = (340 * specs.density).toInt()
        val baseExpandedHeight = (180 * specs.density).toInt()

        val width = if (isExpanded) baseExpandedWidth.coerceAtMost(specs.displayWidth - 32) else baseCompactWidth
        val height = if (isExpanded) baseExpandedHeight else baseCompactHeight

        val centerX = specs.cutoutBoundingRect?.centerX ?: (specs.displayWidth / 2)
        val x = (centerX - width / 2).coerceIn(0, specs.displayWidth - width)

        val top = specs.cutoutBoundingRect?.top ?: specs.statusBarHeight

        return PillBounds(
            x = x,
            top = top,
            width = width,
            height = height,
            isVisible = true
        )
    }
}
