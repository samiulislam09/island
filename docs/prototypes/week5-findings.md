# Week 5 Prototype Findings & Milestone 5 Verification Report

- **Date:** 2026-11-11
- **Status:** Complete & Verified
- **Milestone:** Milestone 5 (Device Compatibility, Capability Flags & OEM Adaptation)

---

## 1. Device Compatibility & Architecture Summary

### 1.1 Architecture & Separation of Concerns
- **Objective:** Enable seamless execution across highly fragmented Android OEM environments, diverse screen cutout geometries, foldable form factors, and device thermal constraints without introducing Android UI coupling in core business logic.
- **Implementation:**
  - `:core:common`: Holds pure Kotlin models (`OemType`, `CutoutType`, `DeviceCapabilities`, `OemRestrictionProfile`), static capability detectors (`DeviceCapabilityDetector`), and reactive thermal monitoring state machines (`ThermalThrottleMonitor`, `ThrottleLevel`). Zero dependency on Android UI or Jetpack Compose.
  - `:overlay`: Consumes device capabilities to dynamically position overlay windows around hardware cutouts (`PositionCalculator.calculateAdaptivePosition`), handles display folding transitions, enforces animation downgrading under thermal/power stress, and maintains leak-free `WindowController` lifecycles.
  - `:feature:settings`: Provides context-aware OEM guidance cards (`OemGuidanceCard`) rendered when proprietary vendor restrictions are detected.
  - `:app`: Establishes automated security audit gates (`ManifestSecurityAuditTest`) ensuring zero declaration of forbidden permissions across the entire project repository.

---

## 2. OEM Detection & Restriction Profiles

### 2.1 Manufacturer & Brand Classification (`DeviceCapabilityDetector`)
- Classifies devices using case-insensitive substring matching against `Build.MANUFACTURER` and `Build.BRAND`:
  - **Samsung:** Matches `"samsung"` -> `OemType.SAMSUNG`.
  - **Xiaomi:** Matches `"xiaomi"`, `"redmi"`, `"poco"` -> `OemType.XIAOMI`.
  - **Oppo / OnePlus:** Matches `"oppo"`, `"oneplus"`, `"realme"` -> `OemType.OPPO_ONEPLUS`.
  - **Vivo:** Matches `"vivo"`, `"iqoo"` -> `OemType.VIVO`.
  - **Motorola / Lenovo:** Matches `"motorola"`, `"lenovo"` -> `OemType.MOTOROLA`.
  - **Stock / Pixel / Other:** Clean Android environments (e.g., Google Pixel 9) -> `OemType.STOCK_OR_OTHER`.

### 2.2 OEM Restriction Profiles (`OemRegistry` & `OemIntentLauncher`)
Vendor-customized Android skins aggressively terminate background overlays and notification listeners. `OemRegistry` maps detected vendors to verified remediation steps and direct settings intents:
- **Xiaomi (HyperOS / MIUI):** Directs users to `com.miui.securitycenter` (`PermissionsEditorActivity`) to enable "Autostart" and "Display pop-up windows while running in the background".
- **Samsung (One UI):** Directs users to battery optimization settings (`android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS`) to set battery usage to "Unrestricted" and remove Island from "Sleeping apps".
- **OnePlus / Oppo (OxygenOS / ColorOS):** Directs users to `com.coloros.safecenter` to enable "Auto-launch", "Background activity", and "Floating windows".
- **Vivo (FuntouchOS / OriginOS):** Directs users to `com.vivo.permissionmanager` to authorize "High Background Power Usage".
- **Stock Android:** Returns `null` profile, gracefully omitting guidance cards on non-restricted devices.
- **Intent Fallback Safety (`OemIntentLauncher`):** When vendor-specific package components are modified or absent in non-standard ROMs, `OemIntentLauncher` catches `ActivityNotFoundException` and `SecurityException` and falls back safely to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, preventing crashes.

---

## 3. Adaptive Position Calculation & Cutout Transitions

### 3.1 Display Cutout Classification
`DeviceCapabilityDetector.classifyCutout` dynamically normalizes cutout metrics using display density (`effectiveDensity = density ?: maxOf(1f, screenWidth / 390f)`):
- **Center Punch (`CENTER_PUNCH`):** Relative horizontal center between 40% and 60% of screen width with `heightDp ≤ 36dp`. On high-DPI screens (e.g. xxxhdpi with density 3.5), standard 112px cutouts accurately normalize to 32dp and resolve as `CENTER_PUNCH`.
- **Teardrop Notch (`TEARDROP_NOTCH`):** Relative center between 40% and 60% with `heightDp > 36dp` and aspect ratio < 2.0.
- **Corner Left Punch (`CORNER_PUNCH_LEFT`):** Relative center < 35% of screen width.
- **Corner Right Punch (`CORNER_PUNCH_RIGHT`):** Relative center > 65% of screen width.
- **Dual Pill (`PILL_DUAL`):** Aspect ratio `width / height ≥ 2.0` or `widthDp > 60dp`.
- **Under-Display / None (`UNDER_DISPLAY_OR_NONE`):** Zero cutout insets or non-positive dimensions.

### 3.2 Adaptive Positioning Algorithm (`PositionCalculator.calculateAdaptivePosition`)
- **Center & Under-Display:** Perfectly centered at `x = (screenWidth - pillWidth) / 2`.
- **Corner Left:** Anchors to the right edge of the cutout with safety padding: `cutoutBounds.right + paddingPx`, avoiding overlap with the physical camera.
- **Corner Right:** Anchors to the left edge of the cutout: `cutoutBounds.left - pillWidth - paddingPx`.
- **Screen Boundary Clamping:** Clamped within `[0, screenWidth - pillWidth]` to prevent off-screen truncation on narrow devices.
- **Foldable Transitions:** Recomputes immediately upon layout configuration changes (e.g., outer display 900px wide -> `x = 250`, unfolding to inner display 1800px wide -> `x = 700`) without requiring background service restarts.

---

## 4. Thermal & Battery Saver Throttle Management

### 4.1 Reactive Monitoring State Machine (`ThermalThrottleMonitor`)
The core state machine in `:core:common` provides thread-safe compound updates (`synchronized(lock)`) over power save states and platform thermal status levels:
- **NORMAL:** Normal thermal status (0–1) and power save mode disabled.
- **LIGHT_THROTTLE:** Power save mode active OR thermal status `MODERATE` (2).
- **SEVERE_THROTTLE:** Thermal status `SEVERE` (3), `CRITICAL` (4), or `EMERGENCY` (5).
*(Note: Android platform listener registration via `OnThermalStatusChangedListener` and `ACTION_POWER_SAVE_MODE_CHANGED` is integrated with the overlay lifecycle in Milestone 6).*

### 4.2 Animation Downgrading & Zero Infinite Loop Churn
- **Equalizer Choreographer Elimination:** `EqualizerBars` completely bypasses `rememberInfiniteTransition` and renders static placeholder bars whenever `isPlaying == false` OR `throttleLevel != NORMAL`. This eliminates continuous frame callbacks and GPU draws during thermal spikes or low battery conditions.
- **Spring Physics to Tween Fallback:** Under throttle conditions, `IslandRenderer` and `IslandPill` replace physics spring computations with lightweight `tween(durationMillis = 150)`, shedding CPU/compositor frame scheduling overhead.

---

## 5. Leak-Free Window Lifecycle (`WindowController`)

### 5.1 Idempotent Lifecycle Guarantees
- Implements state tracking via `isAttached` boolean flag.
- Safe `attach()` ignores redundant attach calls if already attached.
- Safe `detach()` utilizes `try-finally` blocks and guards against `IllegalArgumentException: View not attached to window manager`.
- Rapid attach/detach stress cycles verify zero leaked views or unhandled platform exceptions.

---

## 6. Manifest Security Audit & Policy Compliance (ADR-08 / Privacy)

### 6.1 Prohibited Permissions Verification
An automated test suite (`ManifestSecurityAuditTest`) audits the application manifest and all module manifests across the codebase, asserting zero occurrences of high-risk permissions:
1. `android.permission.INTERNET`: Strictly prohibited. The application performs zero network requests, zero telemetry uploads, and zero remote communication.
2. `android.permission.QUERY_ALL_PACKAGES`: Strictly prohibited. Eliminates Google Play invasive package scraping flags.
3. `android.permission.USE_EXACT_ALARM`: Prohibited. The timer feature utilizes non-privileged scheduling, ensuring store compliance.
4. `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Prohibited. The app uses user-guided settings navigation rather than privileged exemption requests.
5. `android.permission.BIND_ACCESSIBILITY_SERVICE`: Prohibited. The app operates strictly via `NotificationListenerService` and `SYSTEM_ALERT_WINDOW`, maintaining user trust.

---

## 7. Verification Summary

| Component | Module | Status | Evidence |
|---|---|---|---|
| OEM Model & Registry (`OemModels`, `OemRegistry`) | `:core:common` | Passed | `OemRegistryTest` (Xiaomi, Samsung, Oppo, Vivo, Stock) passed |
| Device Capability Classification (`DeviceCapabilityDetector`) | `:core:common` | Passed | `DeviceCapabilityDetectorTest` (OEM heuristic & cutout classification) passed |
| Thermal & Power Save Monitoring (`ThermalThrottleMonitor`) | `:core:common` | Passed | `ThermalThrottleMonitorTest` (Normal, Light, Severe throttle states) passed |
| Adaptive Cutout & Foldable Positioning (`PositionCalculator`) | `:overlay` | Passed | `AdaptivePositionCalculatorTest` (Center, Left, Right, Foldable switch) passed |
| Animation Throttle Guards (`IslandRenderer`, `IslandPill`) | `:overlay` | Passed | `IslandRendererThrottleTest` (Equalizer infinite loop bypassed under throttle) passed |
| Leak-Free Window Lifecycle (`WindowController`) | `:overlay` | Passed | `WindowControllerLifecycleTest` (Idempotent attach/detach, 0 leaks) passed |
| OEM Guidance Cards & Settings (`OemGuidanceCard`, `SettingsDashboard`) | `:feature:settings` | Passed | `SettingsDashboardTest` & `:feature:settings:assembleDebug` passed |
| Automated Manifest Security Audit (`ManifestSecurityAuditTest`) | `:app` | Passed | `ManifestSecurityAuditTest` (2/2 tests passed: app & all module manifests) |
| Full Project Build & Test Suite | All 14 modules | Passed | `./gradlew test :app:assembleDebug` (413 actionable tasks, 0 failures) |
