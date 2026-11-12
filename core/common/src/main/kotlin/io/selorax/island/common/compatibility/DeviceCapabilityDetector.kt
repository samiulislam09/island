package io.selorax.island.common.compatibility

public object DeviceCapabilityDetector {

    public fun classifyOem(manufacturer: String, brand: String): OemType {
        val m = manufacturer.lowercase()
        val b = brand.lowercase()
        return when {
            m.contains("samsung") || b.contains("samsung") -> OemType.SAMSUNG
            m.contains("xiaomi") || m.contains("redmi") || b.contains("poco") -> OemType.XIAOMI
            m.contains("oppo") || m.contains("oneplus") || b.contains("realme") -> OemType.OPPO_ONEPLUS
            m.contains("vivo") || b.contains("iqoo") -> OemType.VIVO
            m.contains("motorola") || m.contains("lenovo") -> OemType.MOTOROLA
            else -> OemType.STOCK_OR_OTHER
        }
    }

    public fun classifyCutout(
        screenWidth: Int,
        screenHeight: Int,
        cutoutLeft: Int,
        cutoutTop: Int,
        cutoutRight: Int,
        cutoutBottom: Int,
        density: Float? = null
    ): CutoutType {
        val cutoutWidth = cutoutRight - cutoutLeft
        val cutoutHeight = cutoutBottom - cutoutTop

        if (cutoutWidth <= 0 || cutoutHeight <= 0 || screenWidth <= 0) {
            return CutoutType.UNDER_DISPLAY_OR_NONE
        }

        val effectiveDensity = if (density != null && density > 0f) {
            density
        } else {
            maxOf(1.0f, screenWidth / 390f)
        }
        val widthDp = cutoutWidth / effectiveDensity
        val heightDp = cutoutHeight / effectiveDensity
        val aspectRatio = cutoutWidth.toFloat() / cutoutHeight.toFloat()

        val centerX = cutoutLeft + (cutoutWidth / 2f)
        val relativeCenterX = centerX / screenWidth.toFloat()

        return when {
            relativeCenterX in 0.40f..0.60f -> {
                when {
                    aspectRatio >= 2.0f || (widthDp > 60f && aspectRatio >= 1.8f) -> CutoutType.PILL_DUAL
                    heightDp > 36f -> CutoutType.TEARDROP_NOTCH
                    else -> CutoutType.CENTER_PUNCH
                }
            }
            relativeCenterX < 0.35f -> CutoutType.CORNER_PUNCH_LEFT
            relativeCenterX > 0.65f -> CutoutType.CORNER_PUNCH_RIGHT
            aspectRatio >= 2.0f || widthDp > 60f -> CutoutType.PILL_DUAL
            else -> CutoutType.CENTER_PUNCH
        }
    }

    public fun createCapabilities(
        manufacturer: String,
        brand: String,
        screenWidth: Int,
        screenHeight: Int,
        cutoutLeft: Int = 0,
        cutoutTop: Int = 0,
        cutoutRight: Int = 0,
        cutoutBottom: Int = 0,
        isFoldable: Boolean = false,
        density: Float? = null
    ): DeviceCapabilities {
        val oem = classifyOem(manufacturer, brand)
        val cutout = classifyCutout(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            cutoutLeft = cutoutLeft,
            cutoutTop = cutoutTop,
            cutoutRight = cutoutRight,
            cutoutBottom = cutoutBottom,
            density = density
        )
        val profile = OemRegistry.getProfile(oem)
        return DeviceCapabilities(
            oemType = oem,
            cutoutType = cutout,
            isFoldable = isFoldable,
            oemProfile = profile
        )
    }

    public fun detect(
        manufacturer: String,
        brand: String,
        screenWidth: Int,
        screenHeight: Int,
        cutoutLeft: Int = 0,
        cutoutTop: Int = 0,
        cutoutRight: Int = 0,
        cutoutBottom: Int = 0,
        isFoldable: Boolean = false,
        density: Float? = null
    ): DeviceCapabilities = createCapabilities(
        manufacturer = manufacturer,
        brand = brand,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        cutoutLeft = cutoutLeft,
        cutoutTop = cutoutTop,
        cutoutRight = cutoutRight,
        cutoutBottom = cutoutBottom,
        isFoldable = isFoldable,
        density = density
    )
}
