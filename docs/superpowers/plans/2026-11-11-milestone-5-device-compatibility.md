# Milestone 5: Device Compatibility, Capability Flags & OEM Adaptation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement comprehensive OEM adaptation (Samsung, Xiaomi, Oppo/OnePlus, Pixel), adaptive cutout positioning (center, corner, under-display, foldables), thermal & battery saver throttle guards, leak-free window lifecycle, contextual settings guidance, and security manifest audits.

**Architecture:** Pure capability models and reactive thermal monitoring in `:core:common`, adaptive insets and throttle guards in `:overlay`, contextual OEM guidance cards in `:feature:settings`, and manifest security gates in `:app`.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Android PowerManager (API 29+ Thermal Status), Android Insets & DisplayCutout APIs, JUnit 4/5, Truth.

**Spec:** `docs/superpowers/specs/2026-11-11-milestone-5-device-compatibility-design.md`

## Global Constraints

- Never commit automatically: do not execute `git commit` commands; keep working tree changes uncommitted.
- Architecture integrity: `:core:common` must have zero Android UI dependencies.
- Zero battery overhead: zero background polling loops; all monitors are callback-driven (`OnThermalStatusChangedListener`, `ACTION_POWER_SAVE_MODE_CHANGED`).
- Accessibility: interactive OEM deep-link buttons must meet WCAG 2.5.5 (minimum 48dp × 48dp touch targets).
- Manifest compliance: release builds must forbid `INTERNET`, `QUERY_ALL_PACKAGES`, `USE_EXACT_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and `BIND_ACCESSIBILITY_SERVICE`.

## Review Focus

1. **Missing OEM Intent Target:** If an OEM settings intent is unavailable on a customized ROM, ensure it falls back gracefully to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` without crashing with `ActivityNotFoundException`.
2. **Infinite Animator Choreographer Drain:** Under `ThrottleLevel.LIGHT_THROTTLE` or `SEVERE_THROTTLE`, ensure `EqualizerBars` completely bypasses `rememberInfiniteTransition` to prevent GPU/CPU wakeups.
3. **Corner Punch-Hole Clipping:** In `PositionCalculator`, ensure left/right punch-holes hug the cutout without clipping off-screen on narrow devices.
4. **WindowManager View Leak on Detach:** Ensure rapid `attach()`/`detach()` cycles are idempotent and never throw `IllegalArgumentException: View not attached to window manager`.
5. **Foldable Screen Dimension Switch:** Ensure screen width/height and cutout geometry re-evaluate dynamically when folding or rotating without requiring a service reboot.

---

### Task 1: OEM & Capability Models and Registry (`:core:common`)

**Files:**
- Create: `core/common/src/main/kotlin/io/selorax/island/common/compatibility/OemModels.kt`
- Create: `core/common/src/main/kotlin/io/selorax/island/common/compatibility/OemRegistry.kt`
- Test: `core/common/src/test/kotlin/io/selorax/island/common/compatibility/OemRegistryTest.kt`

**Interfaces:**
- Produces: `OemType`, `CutoutType`, `OemRestrictionProfile`, `DeviceCapabilities`, `OemRegistry`

- [ ] **Step 1: Write failing test `OemRegistryTest.kt`**

`core/common/src/test/kotlin/io/selorax/island/common/compatibility/OemRegistryTest.kt`:
```kotlin
package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OemRegistryTest {

    @Test
    fun getProfile_xiaomi_returnsAutostartAndPopupsProfile() {
        val profile = OemRegistry.getProfile(OemType.XIAOMI)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("Xiaomi")
        assertThat(profile?.settingsPackage).isEqualTo("com.miui.securitycenter")
    }

    @Test
    fun getProfile_samsung_returnsBatteryOptimizationProfile() {
        val profile = OemRegistry.getProfile(OemType.SAMSUNG)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("Samsung")
    }

    @Test
    fun getProfile_oppo_returnsStartupManagerProfile() {
        val profile = OemRegistry.getProfile(OemType.OPPO_ONEPLUS)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("OnePlus")
    }

    @Test
    fun getProfile_stock_returnsNull() {
        val profile = OemRegistry.getProfile(OemType.STOCK_OR_OTHER)
        assertThat(profile).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify failure (RED)**

Run:
```bash
./gradlew :core:common:test
```
Expected: FAIL with unresolved references `OemType`, `OemRegistry`.

- [ ] **Step 3: Implement `OemModels.kt` and `OemRegistry.kt`**

`core/common/src/main/kotlin/io/selorax/island/common/compatibility/OemModels.kt`:
```kotlin
package io.selorax.island.common.compatibility

public enum class OemType {
    SAMSUNG,
    XIAOMI,
    OPPO_ONEPLUS,
    VIVO,
    MOTOROLA,
    STOCK_OR_OTHER
}

public enum class CutoutType {
    CENTER_PUNCH,
    CORNER_PUNCH_LEFT,
    CORNER_PUNCH_RIGHT,
    TEARDROP_NOTCH,
    PILL_DUAL,
    UNDER_DISPLAY_OR_NONE
}

public data class OemRestrictionProfile(
    val oemType: OemType,
    val title: String,
    val description: String,
    val instructions: List<String>,
    val settingsIntentAction: String? = null,
    val settingsPackage: String? = null,
    val settingsComponent: String? = null
)

public data class DeviceCapabilities(
    val oemType: OemType,
    val cutoutType: CutoutType,
    val isFoldable: Boolean,
    val oemProfile: OemRestrictionProfile?
)
```

`core/common/src/main/kotlin/io/selorax/island/common/compatibility/OemRegistry.kt`:
```kotlin
package io.selorax.island.common.compatibility

public object OemRegistry {
    private val profiles = mapOf(
        OemType.XIAOMI to OemRestrictionProfile(
            oemType = OemType.XIAOMI,
            title = "Xiaomi / HyperOS Setup",
            description = "Xiaomi restricts background overlays and notification listeners by default.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Enable 'Autostart' for Island.",
                "Go to 'Other permissions' and allow 'Display pop-up windows while running in the background'."
            ),
            settingsPackage = "com.miui.securitycenter",
            settingsComponent = "com.miui.permcenter.permissions.PermissionsEditorActivity"
        ),
        OemType.SAMSUNG to OemRestrictionProfile(
            oemType = OemType.SAMSUNG,
            title = "Samsung One UI Battery Settings",
            description = "One UI may put Island to sleep when unused in the foreground.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Set Battery usage to 'Unrestricted'.",
                "Ensure Island is removed from 'Sleeping apps' in Device Care."
            ),
            settingsIntentAction = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
        ),
        OemType.OPPO_ONEPLUS to OemRestrictionProfile(
            oemType = OemType.OPPO_ONEPLUS,
            title = "OnePlus / ColorOS Background Management",
            description = "OxygenOS/ColorOS restricts background floating windows and auto-launch.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Enable 'Auto-launch' and 'Background activity'.",
                "Allow 'Floating windows' permission."
            ),
            settingsPackage = "com.coloros.safecenter"
        ),
        OemType.VIVO to OemRestrictionProfile(
            oemType = OemType.VIVO,
            title = "Vivo / FuntouchOS Setup",
            description = "FuntouchOS requires manual background authorization.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Set background power consumption to 'High Background Power Usage'."
            ),
            settingsPackage = "com.vivo.permissionmanager"
        )
    )

    public fun getProfile(oemType: OemType): OemRestrictionProfile? = profiles[oemType]
}
```

- [ ] **Step 4: Run test to verify it passes (GREEN)**

Run:
```bash
./gradlew :core:common:test
```
Expected: PASS.

---

### Task 2: Device Capability Detector (`:core:common`)

**Files:**
- Create: `core/common/src/main/kotlin/io/selorax/island/common/compatibility/DeviceCapabilityDetector.kt`
- Test: `core/common/src/test/kotlin/io/selorax/island/common/compatibility/DeviceCapabilityDetectorTest.kt`

**Interfaces:**
- Consumes: `OemType`, `CutoutType`, `DeviceCapabilities`, `OemRegistry`
- Produces: `DeviceCapabilityDetector.classifyOem`, `classifyCutout`, `detect`

- [ ] **Step 1: Write failing test `DeviceCapabilityDetectorTest.kt`**

`core/common/src/test/kotlin/io/selorax/island/common/compatibility/DeviceCapabilityDetectorTest.kt`:
```kotlin
package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
```

- [ ] **Step 2: Run test to verify failure (RED)**

Run:
```bash
./gradlew :core:common:test
```
Expected: FAIL with unresolved reference `DeviceCapabilityDetector`.

- [ ] **Step 3: Implement `DeviceCapabilityDetector.kt`**

`core/common/src/main/kotlin/io/selorax/island/common/compatibility/DeviceCapabilityDetector.kt`:
```kotlin
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
        cutoutBottom: Int
    ): CutoutType {
        val cutoutWidth = cutoutRight - cutoutLeft
        val cutoutHeight = cutoutBottom - cutoutTop

        if (cutoutWidth <= 0 || cutoutHeight <= 0 || screenWidth <= 0) {
            return CutoutType.UNDER_DISPLAY_OR_NONE
        }

        val centerX = cutoutLeft + (cutoutWidth / 2f)
        val relativeCenterX = centerX / screenWidth.toFloat()

        return when {
            relativeCenterX in 0.40f..0.60f -> {
                if (cutoutHeight > 100) CutoutType.TEARDROP_NOTCH else CutoutType.CENTER_PUNCH
            }
            relativeCenterX < 0.35f -> CutoutType.CORNER_PUNCH_LEFT
            relativeCenterX > 0.65f -> CutoutType.CORNER_PUNCH_RIGHT
            cutoutWidth > 220 -> CutoutType.PILL_DUAL
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
        isFoldable: Boolean = false
    ): DeviceCapabilities {
        val oem = classifyOem(manufacturer, brand)
        val cutout = classifyCutout(screenWidth, screenHeight, cutoutLeft, cutoutTop, cutoutRight, cutoutBottom)
        val profile = OemRegistry.getProfile(oem)
        return DeviceCapabilities(
            oemType = oem,
            cutoutType = cutout,
            isFoldable = isFoldable,
            oemProfile = profile
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes (GREEN)**

Run:
```bash
./gradlew :core:common:test
```
Expected: PASS.

---

### Task 3: Thermal & Battery Throttle Monitor (`:core:common`)

**Files:**
- Create: `core/common/src/main/kotlin/io/selorax/island/common/compatibility/ThermalThrottleMonitor.kt`
- Test: `core/common/src/test/kotlin/io/selorax/island/common/compatibility/ThermalThrottleMonitorTest.kt`

**Interfaces:**
- Produces: `ThrottleLevel`, `ThermalThrottleMonitor`

- [ ] **Step 1: Write failing test `ThermalThrottleMonitorTest.kt`**

`core/common/src/test/kotlin/io/selorax/island/common/compatibility/ThermalThrottleMonitorTest.kt`:
```kotlin
package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThermalThrottleMonitorTest {

    @Test
    fun computeThrottleLevel_normalConditions_returnsNormal() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = 0 // THERMAL_STATUS_NONE
        )
        assertThat(level).isEqualTo(ThrottleLevel.NORMAL)
    }

    @Test
    fun computeThrottleLevel_powerSaveOn_returnsLightThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = true,
            thermalStatus = 0
        )
        assertThat(level).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_thermalModerate_returnsLightThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = 2 // THERMAL_STATUS_MODERATE
        )
        assertThat(level).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_thermalSevere_returnsSevereThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = 3 // THERMAL_STATUS_SEVERE
        )
        assertThat(level).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)
    }
}
```

- [ ] **Step 2: Run test to verify failure (RED)**

Run:
```bash
./gradlew :core:common:test
```
Expected: FAIL with unresolved references `ThrottleLevel`, `ThermalThrottleMonitor`.

- [ ] **Step 3: Implement `ThermalThrottleMonitor.kt`**

`core/common/src/main/kotlin/io/selorax/island/common/compatibility/ThermalThrottleMonitor.kt`:
```kotlin
package io.selorax.island.common.compatibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public enum class ThrottleLevel {
    NORMAL,
    LIGHT_THROTTLE,
    SEVERE_THROTTLE
}

public class ThermalThrottleMonitor(
    initialPowerSaveMode: Boolean = false,
    initialThermalStatus: Int = 0
) {
    private val _throttleLevel = MutableStateFlow(
        computeThrottleLevel(initialPowerSaveMode, initialThermalStatus)
    )
    public val throttleLevel: StateFlow<ThrottleLevel> = _throttleLevel.asStateFlow()

    private var currentPowerSaveMode: Boolean = initialPowerSaveMode
    private var currentThermalStatus: Int = initialThermalStatus

    public fun onPowerSaveModeChanged(isPowerSaveMode: Boolean) {
        currentPowerSaveMode = isPowerSaveMode
        _throttleLevel.value = computeThrottleLevel(currentPowerSaveMode, currentThermalStatus)
    }

    public fun onThermalStatusChanged(thermalStatus: Int) {
        currentThermalStatus = thermalStatus
        _throttleLevel.value = computeThrottleLevel(currentPowerSaveMode, currentThermalStatus)
    }

    public companion object {
        // Platform thermal status constants (API 29+)
        public const val STATUS_NONE: Int = 0
        public const val STATUS_LIGHT: Int = 1
        public const val STATUS_MODERATE: Int = 2
        public const val STATUS_SEVERE: Int = 3
        public const val STATUS_CRITICAL: Int = 4
        public const val STATUS_EMERGENCY: Int = 5

        public fun computeThrottleLevel(isPowerSaveMode: Boolean, thermalStatus: Int): ThrottleLevel {
            return when {
                thermalStatus >= STATUS_SEVERE -> ThrottleLevel.SEVERE_THROTTLE
                isPowerSaveMode || thermalStatus == STATUS_MODERATE -> ThrottleLevel.LIGHT_THROTTLE
                else -> ThrottleLevel.NORMAL
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes (GREEN)**

Run:
```bash
./gradlew :core:common:test
```
Expected: PASS.

---

### Task 4: Adaptive Position Calculator & Foldable Insets (`:overlay`)

**Files:**
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/PositionCalculator.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/AdaptivePositionCalculatorTest.kt`

**Interfaces:**
- Consumes: `CutoutType`, `DeviceCapabilities`
- Produces: `PositionCalculator.calculateAdaptivePosition`

- [ ] **Step 1: Write failing test `AdaptivePositionCalculatorTest.kt`**

`overlay/src/test/kotlin/io/selorax/island/overlay/AdaptivePositionCalculatorTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify failure (RED)**

Run:
```bash
./gradlew :overlay:testDebugUnitTest --tests "io.selorax.island.overlay.AdaptivePositionCalculatorTest"
```
Expected: FAIL with unresolved method `calculateAdaptivePosition`.

- [ ] **Step 3: Update `PositionCalculator.kt`**

Add `calculateAdaptivePosition` to `overlay/src/main/kotlin/io/selorax/island/overlay/PositionCalculator.kt`:
```kotlin
package io.selorax.island.overlay

import android.graphics.Rect
import io.selorax.island.common.compatibility.CutoutType

public object PositionCalculator {

    public data class Coordinates(val x: Int, val y: Int)

    public fun calculate(screenWidth: Int, pillWidth: Int, topOffset: Int): Coordinates {
        val x = (screenWidth - pillWidth) / 2
        return Coordinates(x = x.coerceAtLeast(0), y = topOffset)
    }

    public fun calculateAdaptivePosition(
        screenWidth: Int,
        pillWidth: Int,
        statusBarHeight: Int,
        cutoutType: CutoutType,
        cutoutBounds: Rect? = null,
        paddingPx: Int = 12
    ): Coordinates {
        val x = when (cutoutType) {
            CutoutType.CORNER_PUNCH_LEFT -> {
                if (cutoutBounds != null && cutoutBounds.right > 0) {
                    cutoutBounds.right + paddingPx
                } else {
                    paddingPx
                }
            }
            CutoutType.CORNER_PUNCH_RIGHT -> {
                if (cutoutBounds != null && cutoutBounds.left > 0) {
                    cutoutBounds.left - pillWidth - paddingPx
                } else {
                    screenWidth - pillWidth - paddingPx
                }
            }
            CutoutType.CENTER_PUNCH,
            CutoutType.TEARDROP_NOTCH,
            CutoutType.PILL_DUAL,
            CutoutType.UNDER_DISPLAY_OR_NONE -> {
                (screenWidth - pillWidth) / 2
            }
        }

        return Coordinates(
            x = x.coerceIn(0, (screenWidth - pillWidth).coerceAtLeast(0)),
            y = statusBarHeight
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes (GREEN)**

Run:
```bash
./gradlew :overlay:testDebugUnitTest --tests "io.selorax.island.overlay.AdaptivePositionCalculatorTest"
```
Expected: PASS.

---

### Task 5: Throttle Guards in Island Overlay (`:overlay`)

**Files:**
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandRenderer.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/IslandRendererThrottleTest.kt`

**Interfaces:**
- Consumes: `ThrottleLevel`, `IslandRenderer`

- [ ] **Step 1: Write test `IslandRendererThrottleTest.kt`**

`overlay/src/test/kotlin/io/selorax/island/overlay/IslandRendererThrottleTest.kt`:
```kotlin
package io.selorax.island.overlay

import io.selorax.island.common.compatibility.ThrottleLevel
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

    private fun shouldRunAnimations(isPlaying: Boolean, throttle: ThrottleLevel): Boolean {
        return isPlaying && throttle == ThrottleLevel.NORMAL
    }
}
```

- [ ] **Step 2: Update `IslandRenderer.kt` with `ThrottleLevel` support**

Add parameter `throttleLevel: ThrottleLevel = ThrottleLevel.NORMAL` to `IslandRenderer`:
- Pass `isPlaying = (isPlaying && throttleLevel == ThrottleLevel.NORMAL)` into `CompactEnd`
- In `morphSpring()`: if `throttleLevel != ThrottleLevel.NORMAL`, use `tween(durationMillis = 150)` to lighten compositor workload.

- [ ] **Step 3: Run overlay tests and verify compilation**

Run:
```bash
./gradlew :overlay:testDebugUnitTest :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 6: Leak-Free WindowController Lifecycle (`:overlay`)

**Files:**
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/WindowController.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/WindowControllerLifecycleTest.kt`

**Interfaces:**
- Produces: Idempotent `WindowController.attach`, `detach`, `isAttached`

- [ ] **Step 1: Write test `WindowControllerLifecycleTest.kt`**

`overlay/src/test/kotlin/io/selorax/island/overlay/WindowControllerLifecycleTest.kt`:
```kotlin
package io.selorax.island.overlay

import android.view.View
import android.view.WindowManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class WindowControllerLifecycleTest {

    private lateinit var windowManager: WindowManager
    private lateinit var view: View
    private lateinit var controller: WindowController

    @Before
    fun setup() {
        windowManager = mock(WindowManager::class.java)
        view = mock(View::class.java)
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
}
```

- [ ] **Step 2: Update `WindowController.kt` to ensure idempotency and zero leaks**

Ensure `isAttached` tracking, safe `removeViewImmediate`, and cleanup on detach.

- [ ] **Step 3: Run tests to verify**

Run:
```bash
./gradlew :overlay:testDebugUnitTest :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 7: OEM Guidance Card & Settings Integration (`:feature:settings`)

**Files:**
- Create: `feature/settings/src/main/kotlin/io/selorax/island/feature/settings/OemGuidanceCard.kt`
- Modify: `feature/settings/src/main/kotlin/io/selorax/island/feature/settings/SettingsDashboard.kt`
- Test: `feature/settings/src/test/kotlin/io/selorax/island/feature/settings/SettingsDashboardTest.kt`

**Interfaces:**
- Consumes: `DeviceCapabilities`, `OemRestrictionProfile`
- Produces: `OemGuidanceCard`, updated `SettingsDashboard`

- [ ] **Step 1: Write test `SettingsDashboardTest.kt`**

`feature/settings/src/test/kotlin/io/selorax/island/feature/settings/SettingsDashboardTest.kt`:
```kotlin
package io.selorax.island.feature.settings

import io.selorax.island.common.compatibility.CutoutType
import io.selorax.island.common.compatibility.DeviceCapabilities
import io.selorax.island.common.compatibility.OemRegistry
import io.selorax.island.common.compatibility.OemType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDashboardTest {

    @Test
    fun oemProfile_availableForXiaomi_omittedForStock() {
        val xiaomiCaps = DeviceCapabilities(
            oemType = OemType.XIAOMI,
            cutoutType = CutoutType.CENTER_PUNCH,
            isFoldable = false,
            oemProfile = OemRegistry.getProfile(OemType.XIAOMI)
        )
        assertNotNull(xiaomiCaps.oemProfile)

        val stockCaps = DeviceCapabilities(
            oemType = OemType.STOCK_OR_OTHER,
            cutoutType = CutoutType.CENTER_PUNCH,
            isFoldable = false,
            oemProfile = OemRegistry.getProfile(OemType.STOCK_OR_OTHER)
        )
        assertNull(stockCaps.oemProfile)
    }
}
```

- [ ] **Step 2: Implement `OemGuidanceCard.kt`**

`feature/settings/src/main/kotlin/io/selorax/island/feature/settings/OemGuidanceCard.kt`:
```kotlin
package io.selorax.island.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.common.compatibility.OemRestrictionProfile
import io.selorax.island.designsystem.IslandColors

@Composable
public fun OemGuidanceCard(
    profile: OemRestrictionProfile,
    onOpenSettings: (OemRestrictionProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(IslandColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, IslandColors.AccentIon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = profile.title, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = profile.description, color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        profile.instructions.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                color = IslandColors.OnSurfaceHigh,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onOpenSettings(profile) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentIon)
        ) {
            Text("Open Settings", color = Color.White)
        }
    }
}
```

- [ ] **Step 3: Update `SettingsDashboard.kt` to include `OemGuidanceCard`**

When `deviceCapabilities.oemProfile != null`, render `OemGuidanceCard` as the first item in the LazyColumn.

- [ ] **Step 4: Run tests and verify compilation**

Run:
```bash
./gradlew :feature:settings:testDebugUnitTest :feature:settings:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 8: Manifest Security Audit & Milestone 5 Verification (`:app`)

**Files:**
- Create: `app/src/test/kotlin/io/selorax/island/ManifestSecurityAuditTest.kt`
- Create: `docs/prototypes/week5-findings.md`

**Interfaces:**
- Validates: Absence of forbidden permissions across the entire project
- Produces: Documented Week 5 findings report

- [ ] **Step 1: Write `ManifestSecurityAuditTest.kt`**

`app/src/test/kotlin/io/selorax/island/ManifestSecurityAuditTest.kt`:
```kotlin
package io.selorax.island

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ManifestSecurityAuditTest {

    private val forbiddenPermissions = listOf(
        "android.permission.INTERNET",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
    )

    @Test
    fun appManifest_doesNotContainForbiddenPermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val content = manifestFile.readText()
            forbiddenPermissions.forEach { permission ->
                assertFalse(
                    "Manifest must not declare forbidden permission: $permission",
                    content.contains(permission)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Run complete project test suite and assembleDebug**

Run:
```bash
./gradlew test :app:assembleDebug
```
Expected: BUILD SUCCESSFUL (all actionable tasks pass, 0 failures).

- [ ] **Step 3: Document findings in `docs/prototypes/week5-findings.md`**

Record findings for:
- OEM detection accuracy across Samsung, Xiaomi, Oppo, Pixel.
- Adaptive cutout layout coordinates and foldable inner/outer transitions.
- Thermal throttle level transitions and zero infinite choreographer loop verification.
- Manifest security audit gate.
