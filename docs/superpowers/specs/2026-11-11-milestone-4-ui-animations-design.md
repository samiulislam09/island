# Milestone 4: UI, Animations & Customization Design Specification

- **Date:** 2026-11-11
- **Status:** Proposed (Design Approved)
- **Target:** Milestone 4 (Week 4 of Development Roadmap)
- **Branch:** `feature/milestone-1-foundation-overlay` (no auto-commit per user constraint)

---

## 1. Scope & Objectives

Milestone 3 built the engine (actor model, priority policy, event processor, deadline scheduler). Milestone 4 delivers the complete visual, motion, and user-facing layer:
1. **Design System (`:core:designsystem`)**:
   - Design tokens: Colors (`#0B0B0C` near-black surface, `#5B8CFF` Ion accent), typography with tabular numbers (`tnum`), and physical spring specs.
   - Component library: `IslandSurface`, `IslandProgressBar`, `EqualizerBars`, `RingProgress`, `SettingsSwitchRow`, `SettingsSliderRow`.
2. **Specialized Activity Renderers**:
   - Music (album art, equalizer animation, seek bar, playback controls).
   - Battery (animated charging bolt, ring progress, low battery saver).
   - Notification (app icon, 2-line title, multi-line text, lock-screen redaction).
   - Timer (tabular countdown, progress ring, pause/resume/cancel controls).
3. **Fluid Spring Choreography**:
   - Smooth container morphing with interruptible `Animatable` springs (`motion.morph`, `motion.content`, `motion.alert`).
   - Reduced motion mode auto-detection (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`).
4. **Companion App Screens (`:feature:onboarding` & `:feature:settings`)**:
   - 3-step Onboarding with live preview and Google Play policy-compliant Prominent Disclosure.
   - Settings Dashboard with master toggle, position offsets, behavior toggles, and live reactivity.

---

## 2. Architecture & Design Tokens

### 2.1 Tokens in `:core:designsystem`

#### Colors (`IslandColors`)
```kotlin
public object IslandColors {
    public val Surface: Color = Color(0xFF0B0B0C)
    public val SurfaceLight: Color = Color(0xFFF2F2F4)
    public val OnSurfaceHigh: Color = Color(0xEBFFFFFF)   // 92%
    public val OnSurfaceMedium: Color = Color(0xA3FFFFFF) // 64%
    public val AccentIon: Color = Color(0xFF5B8CFF)
    public val Danger: Color = Color(0xFFFF5A5F)
    public val Success: Color = Color(0xFF3DDC84)
}
```

#### Motion (`IslandMotion`)
```kotlin
public object IslandMotion {
    public val morphSpring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )
    public val contentSpring = spring<Float>(
        dampingRatio = 1.0f,
        stiffness = 600f
    )
    public val alertSpring = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = 300f
    )
}
```

#### Typography
- Tabular figures (`FontFeature.tabularFigures()`) enabled on time strings and percentages to eliminate width jitter during ticks.
- Overlay text scale clamped to ≤ 1.3× to preserve pill geometry on large display zoom settings.

---

### 2.2 Foundational Composables (`:core:designsystem`)

1. **`IslandSurface`**:
   - Pill background container with animateContentSize / morph spring.
   - Near-black `#0B0B0C` background with 24-28 dp rounded corners.
2. **`EqualizerBars`**:
   - 3 vertical bars with phase-shifted infinite animations representing music playback. Paused when playback is paused or view is hidden.
3. **`RingProgress`**:
   - Circular arc drawn on Canvas showing remaining duration for timers or current charge level for battery.
4. **`IslandProgressBar`**:
   - 4 dp track with accent fill and optional interactive scrubber.
5. **`SettingsRow` Components**:
   - `SettingsSection`, `SettingsSwitchRow`, `SettingsSliderRow`, `SettingsNavRow` conforming to Material 3 guidelines.

---

### 2.3 Activity Renderers (`:overlay`)

An extensible renderer contract:
```kotlin
public interface ActivityRenderer {
    public val source: EventSource
    @Composable public fun CompactStart(activity: LiveActivity)
    @Composable public fun CompactEnd(activity: LiveActivity)
    @Composable public fun Expanded(activity: LiveActivity, onAction: (String) -> Unit)
}
```

- **Music Renderer**:
  - Compact: 22 dp rounded album artwork on start, `EqualizerBars` on end.
  - Expanded: 56 dp artwork, title, artist, seek bar with times, prev/play/next buttons.
- **Battery Renderer**:
  - Compact: Bolt icon on start, `XX%` text on end.
  - Expanded: `RingProgress` with battery percentage and charging status.
- **Notification Renderer**:
  - Compact: App icon on start, title preview on end.
  - Expanded: App name + icon, title, multi-line body, "Open" and "Dismiss" chips.
  - Redacted mode: Shows "New notification" on lock screen without displaying sensitive text.
- **Timer Renderer**:
  - Compact: Timer icon on start, `MM:SS` (tabular) on end.
  - Expanded: Big `MM:SS` clock, `RingProgress`, Pause/Resume, +1m, and Cancel chips.

---

### 2.4 Onboarding & Settings UI

#### Onboarding (`:feature:onboarding`)
- **Page 1: Welcome** — Interactive live island demo.
- **Page 2: Customization** — Size and positioning slider preview.
- **Page 3: Privacy & Permissions** — Strict privacy guarantee + **Prominent Disclosure** for NLS before navigating to system settings.

#### Settings (`:feature:settings`)
- **Dashboard**: On/Off master switch, live preview mock, quick toggles for signal types.
- **Position & Size**: Anchor selection (Cutout / Top center / Below status bar), X and Y offset sliders, compact height and width sliders.
- **Reactivity**: Writes directly to `SettingsRepository`, updating overlay layout in real time.

---

## 3. Verification Strategy

1. **Unit & Screenshot Tests**:
   - `IslandDesignSystemTest`: Validate token values and spring specs.
   - `RendererTests`: Verify Compose rendering of Music, Battery, Notification, and Timer across Compact and Expanded states.
   - `SettingsViewModelTest`: Validate settings mutations and persistence.
2. **Build Integrity**:
   - `./gradlew test` across all 14 modules.
   - `:overlay:assembleDebug` and `:app:assembleDebug` verification.
