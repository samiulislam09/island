package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DeviceCapabilityDetectorTest {

    @Test
    fun classifyOem_recognizesManufacturers() {
        assertThat(DeviceCapabilityDetector.classifyOem("samsung", "samsung")).isEqualTo(OemType.SAMSUNG)
        assertThat(DeviceCapabilityDetector.classifyOem("Xiaomi", "POCO")).isEqualTo(OemType.XIAOMI)
        assertThat(DeviceCapabilityDetector.classifyOem("OnePlus", "OnePlus")).isEqualTo(OemType.OPPO_ONEPLUS)
        assertThat(DeviceCapabilityDetector.classifyOem("OPPO", "OPPO")).isEqualTo(OemType.OPPO_ONEPLUS)
        assertThat(DeviceCapabilityDetector.classifyOem("vivo", "iQOO")).isEqualTo(OemType.VIVO)
        assertThat(DeviceCapabilityDetector.classifyOem("motorola", "motorola")).isEqualTo(OemType.MOTOROLA)
        assertThat(DeviceCapabilityDetector.classifyOem("Google", "Pixel 9")).isEqualTo(OemType.STOCK_OR_OTHER)
    }

    @Test
    fun classifyCutout_centerPunch_classifiedCorrectly() {
        // Screen width 1080, cutout at x=510, width=60, height=60 -> center is 540 (exactly 50%)
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 510,
            cutoutTop = 0,
            cutoutRight = 570,
            cutoutBottom = 60
        )
        assertThat(type).isEqualTo(CutoutType.CENTER_PUNCH)
    }

    @Test
    fun classifyCutout_cornerLeftPunch_classifiedCorrectly() {
        // Cutout on left edge: x=60, width=60 -> center is 90 (< 35% of 1080)
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 60,
            cutoutTop = 0,
            cutoutRight = 120,
            cutoutBottom = 60
        )
        assertThat(type).isEqualTo(CutoutType.CORNER_PUNCH_LEFT)
    }

    @Test
    fun classifyCutout_cornerRightPunch_classifiedCorrectly() {
        // Cutout on right edge: x=960, width=60 -> center is 990 (> 65% of 1080)
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 960,
            cutoutTop = 0,
            cutoutRight = 1020,
            cutoutBottom = 60
        )
        assertThat(type).isEqualTo(CutoutType.CORNER_PUNCH_RIGHT)
    }

    @Test
    fun classifyCutout_teardropNotch_classifiedCorrectly() {
        // Centered cutout with height > 36dp (width 100, height 110)
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 490,
            cutoutTop = 0,
            cutoutRight = 590,
            cutoutBottom = 110,
            density = 1.0f
        )
        assertThat(type).isEqualTo(CutoutType.TEARDROP_NOTCH)
    }

    @Test
    fun classifyCutout_highDensityCenterPunch_doesNotMisclassifyAsTeardrop() {
        // On xxxhdpi (density 3.5), 32dp punch hole is 112px high.
        // It must be classified as CENTER_PUNCH, not TEARDROP_NOTCH.
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1440,
            screenHeight = 3120,
            cutoutLeft = 664,
            cutoutTop = 0,
            cutoutRight = 776,
            cutoutBottom = 112,
            density = 3.5f
        )
        assertThat(type).isEqualTo(CutoutType.CENTER_PUNCH)
    }

    @Test
    fun classifyCutout_pillDual_classifiedCorrectly() {
        // Centered pill cutout with width > 220
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 415,
            cutoutTop = 0,
            cutoutRight = 665,
            cutoutBottom = 70
        )
        assertThat(type).isEqualTo(CutoutType.PILL_DUAL)
    }

    @Test
    fun classifyCutout_noCutout_returnsUnderDisplayOrNone() {
        val type = DeviceCapabilityDetector.classifyCutout(
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 0,
            cutoutTop = 0,
            cutoutRight = 0,
            cutoutBottom = 0
        )
        assertThat(type).isEqualTo(CutoutType.UNDER_DISPLAY_OR_NONE)
    }

    @Test
    fun createCapabilities_assemblesCapabilitiesCorrectly() {
        val caps = DeviceCapabilityDetector.createCapabilities(
            manufacturer = "Xiaomi",
            brand = "POCO",
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 510,
            cutoutTop = 0,
            cutoutRight = 570,
            cutoutBottom = 60,
            isFoldable = false
        )
        assertThat(caps.oemType).isEqualTo(OemType.XIAOMI)
        assertThat(caps.cutoutType).isEqualTo(CutoutType.CENTER_PUNCH)
        assertThat(caps.isFoldable).isFalse()
        assertThat(caps.oemProfile).isNotNull()
        assertThat(caps.oemProfile?.title).contains("Xiaomi")
    }

    @Test
    fun detect_returnsMatchingDeviceCapabilities() {
        val caps = DeviceCapabilityDetector.detect(
            manufacturer = "Google",
            brand = "Pixel 9",
            screenWidth = 1080,
            screenHeight = 2400,
            cutoutLeft = 510,
            cutoutTop = 0,
            cutoutRight = 570,
            cutoutBottom = 60,
            isFoldable = true
        )
        assertThat(caps.oemType).isEqualTo(OemType.STOCK_OR_OTHER)
        assertThat(caps.cutoutType).isEqualTo(CutoutType.CENTER_PUNCH)
        assertThat(caps.isFoldable).isTrue()
        assertThat(caps.oemProfile).isNull()
    }
}
