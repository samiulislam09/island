# UI / UX Specification — Island

Version 0.1 · 2026-11-11 · Status: Draft for approval

---

## 1. Design principles

1. **Glanceable first.** The compact pill communicates the state in < 300 ms of looking: one icon plus one value per lane.
2. **Hardware-honest.** The island grows *around* the camera. It never pretends the camera isn't there.
3. **Physical motion.** Every size change is a spring. Nothing is linear and nothing snaps. Motion respects the system animator scale and reduced-motion settings.
4. **Quiet by default.** No auto-expand, short previews, and one haptic tick per meaningful event.
5. **Two separate products in one APK.** The **island** (overlay, system-level, tiny) and the **app** (settings) share tokens but not code paths. The island never depends on the app being open.

---

## 2. The island

### 2.1 Presentation states

```
 Minimal            Compact                         Compact split                  Expanded
 ┌───┐   (●)        ╭──────────(●)──────────╮       ╭───────(●)──────╮ ╭──╮        ╭──────────────────────────────╮
 │ ● │              │ ♫ art   (cam)   ▶ 2:31│       │ ♫ art (cam) ▮▮ │ │⏱│        │ [art]  Song title            │
 └───┘              ╰───────────────────────╯       ╰────────────────╯ ╰──╯        │        Artist · App          │
 dot next to cam    left lane | cutout | right lane  primary pill + bubble        │ ━━━━━━━●──────── 1:12 / 3:40 │
                                                                                   │     ⏮      ⏯      ⏭          │
                                                                                   ╰──────────────────────────────╯
```

| State | Size (default) | Content |
|---|---|---|
| Hidden | 0 | — (window `GONE`) |
| Minimal | Cutout width + 16 dp, height = compact height | Single status dot/glyph (e.g., charging bolt) |
| Compact | min 120 dp wide, 34 dp high, grows to content up to 60 % of screen width | Left lane: identity (artwork, app icon, bolt). Right lane: value (time, %, waveform) |
| Compact split | Primary compact + 34 dp circular bubble, 6 dp gap | Secondary activity glyph only |
| Expanded | 92 % screen width (max 420 dp), height by content (88–220 dp) | Full activity layout with actions |

**Lane rule:** content never renders under the camera. `PositionCalculator` exposes `cutoutRectInPill`, and the compact layout places lanes left and right of it. On devices without a cutout, the lanes are adjacent with a 12 dp gap.

### 2.2 Per-activity layouts

| Activity | Compact left | Compact right | Expanded |
|---|---|---|---|
| Music | Album art 22 dp (rounded 6 dp) | Animated equalizer bars (playing) / pause glyph | Art 56 dp, title (1 line, marquee off), artist · app, seek bar with times, prev/play/next, open-app chip |
| Battery (charging) | Bolt icon animated fill | `78 %` | Large ring with %, "Charging · Fast" (if detectable via plug type and `BatteryManager` current, best-effort), time-to-full (API 28 `computeChargeTimeRemaining`, may be −1) |
| Battery (low/full) | Battery glyph (red/green) | `15 %` | Message + "Battery saver" action (opens settings) |
| Notification | App icon 20 dp | Title (truncate) | App icon + name + time, title (2 lines), text (up to 4 lines), actions: Open, Dismiss |
| Notification (redacted) | App icon | "New notification" | App name, "Content hidden" + Open |
| Timer | Timer glyph with progress ring | `04:59` (tabular figures) | Big time, progress bar, Pause/Resume, +1 min, Cancel |
| Stopwatch | Stopwatch glyph | `12:03` | Big time (with 1/10 s while expanded only), Pause/Resume, Reset |

### 2.3 Gestures and interaction

| Gesture | On compact | On expanded | Feedback |
|---|---|---|---|
| Tap | Expand primary | Tap on content = primary action (open notification/app). Tap on button = action | Haptic `CONTEXT_CLICK`, scale 0.96 press state |
| Long press (400 ms) | Quick menu: Open app · Hide 1 h · Hide this app's notifications · Settings for this activity | Same | Haptic `LONG_PRESS`, menu morphs out of the pill |
| Swipe up | Snooze island 1 min (configurable) | Collapse | Rubber-band follow, then spring |
| Swipe down | Expand | — (no-op, rubber band) | |
| Swipe left/right | Cycle primary ↔ secondary/queued | Same (with page dots if > 1) | Content slides with velocity carry-over |
| Tap outside | — | Collapse (via `FLAG_WATCH_OUTSIDE_TOUCH`; outside tap still reaches the other app) | |
| Drag seek bar | — | Seek (if supported) | Thumb grows 1.5×. Time label follows |

**Gesture thresholds:** touch slop = `ViewConfiguration.scaledTouchSlop`. Swipe commit = 24 dp distance or 600 dp/s velocity.

**Accessibility in the island:**
- Every action has a `contentDescription`.
- A TalkBack user can focus the pill. Double-tap expands, and custom accessibility actions provide "Dismiss" and "Next activity".
- A minimum 48 dp touch target is achieved with invisible padding inside the window slop.

### 2.4 Motion specification

All motion uses `spring()` specs from `core:designsystem` tokens:

| Token | dampingRatio | stiffness | Use |
|---|---|---|---|
| `motion.morph` | 0.82 | 380 (Standard) | Pill size/shape between states |
| `motion.content` | 1.0 | 600 | Content fade/slide inside pill |
| `motion.press` | 0.6 | 1500 | Press scale |
| `motion.alert` | 0.55 | 300 | Bounce on new alert (single overshoot) |

| Motion preset | Stiffness multiplier | Notes |
|---|---|---|
| Relaxed | 0.6 | |
| Standard | 1.0 | Default |
| Snappy | 1.6 | |
| Reduced | — | Cross-fade 150 ms, no scale/overshoot. Auto-selected when `Settings.Global.ANIMATOR_DURATION_SCALE == 0` or the "remove animations" accessibility setting is on |
| Elastic (Pro) | 0.8, damping 0.6 | Visible overshoot |
| Liquid (Pro) | Custom path morph (metaball-like corner blend between pill and bubble) | Must pass NFR-02 on the reference mid-range device to ship |

**Choreography (compact → expanded):**
1. t=0: window grows to union bounds (1 relayout).
2. Pill bounds spring to target. Compact content fades out over 90 ms.
3. Expanded content fades in and slides 8 dp down after the pill reaches 60 % of the target height (`snapshotFlow` on progress).
4. On settle: window shrinks to the final bounds (1 relayout, no-op if equal).

Interruption: a new target mid-animation retargets `Animatable` with the current velocity. The window union recalculates from the *current* bounds.

**Alert entrance** (new notification while compact): the pill widens with `motion.alert`, the content cross-fades, and one light haptic plays. If hidden, the pill scales from 0.6 → 1 with fade at the anchor.

**Charging animation:** 1.2 s energy sweep across the pill's background (shader gradient translate), then settles to compact battery. Under the Reduced preset: a static bolt plus a fade.

### 2.5 Island theming
- The pill's default background is near-black `#0B0B0C` in both light and dark themes, because it visually merges with the camera hole. Light mode affects the **app**, plus an optional "Light pill" theme.
- Content colors are on-surface high/medium emphasis. The accent is used for progress and active states only.
- **Artwork-adaptive (Pro):** extract a dominant color with `androidx.palette` on the downscaled artwork (on a background thread, cached per track), and apply it as a subtle gradient. Contrast against content color must be ≥ 4.5:1, otherwise darken.

---

## 3. App screens

Navigation: single Activity, Compose Navigation (type-safe routes), and a bottom bar with **Home · Activities · Customize · Settings**. Onboarding is a separate graph shown until it completes.

### 3.1 Onboarding (3 pages + permission flow)
| Page | Content | Primary CTA |
|---|---|---|
| 1 Welcome | Animated live demo of the island morphing through music → charging → notification (rendered in-app with the same renderer) | "Get started" |
| 2 Your island, your way | Customization preview with a draggable size slider | "Next" |
| 3 Private by design | "Everything happens on your phone. No accounts. No tracking. Notification content is never stored." | "Set up permissions" |

**Acceptance:** skippable with "Skip"; total ≤ 30 s; no permission requested before page 3.

### 3.2 Permission setup
A vertical checklist. Each row shows its state (✓ / required / optional), with a short "why" and a "Grant" button:

1. **Display over other apps** — *Required.* Explanation → system screen. On return, the state refreshes; if still missing: "Looks like it's not on yet — find **Island** in the list and turn it on."
2. **Notification access** — *Recommended.* **Prominent disclosure screen** (full screen, before the system settings): what is read (notification title, text, app; media playback info), what isn't done (stored, shared, sent), "You can turn this off anytime." Buttons: "Allow access" / "Not now". Declining continues with battery + timer.
3. **Notifications** (33+) — *Optional.* Requested in context (first timer).
4. **Alarms & reminders** — *Optional.* Requested in context (first timer > 1 min).
5. **Device-specific setup** — *Shown only on Xiaomi, OPPO/Realme/OnePlus, Samsung.* Step-by-step cards with deep links and fallbacks. "Test it" button: hides the app and shows a test activity after 10 s.

**Acceptance:** each row's state is correct within 500 ms of returning from settings. The flow never dead-ends. Every row is reachable later from Settings → Permissions.

### 3.3 Home (dashboard)
- **Island status card:** On/Off master switch, current state ("Running" / "Paused: missing permission X" / "Snoozed until 14:05"), and a "Fix" action when degraded.
- **Live preview:** the actual renderer in a phone-frame mock, showing the current real state or a demo state.
- **Quick toggles:** Music · Battery · Notifications · Timer chips.
- **Quick actions:** Start timer (preset chips 1/5/10/25 min + custom) and Start stopwatch.
- **Reliability tile** (only if a kill was detected): "Island was stopped by the system 3× this week — Improve reliability."

### 3.4 Island customization
- Sticky live preview at the top (the real overlay also updates live while this screen is open).
- Sections: **Position** (anchor: Camera / Top center / Below status bar; X/Y offset sliders with ±1 dp nudge buttons), **Size** (compact height, min width, expanded width), **Shape** (corner radius slider; Pro: squircle toggle), **Motion** (preset segmented control + "Preview" button that plays the full choreography), **Behavior** (auto-collapse time, hide in landscape, hide in full screen, split pill).
- "Reset section" per section.

### 3.5 Activity management
- Reorderable list (drag handle) of activity types. Order = priority. Each row has an enable switch and a chevron to details.
- **Music detail:** show when paused (linger seconds), layout (Pro: large art), artwork-adaptive (Pro).
- **Battery detail:** charging animation on/off, low threshold slider (5–30 %), full alert toggle.
- **Notifications detail:** default rule, preview duration, show ongoing, "Dismiss also clears notification", and **Per-app rules** (searchable app list, `<queries>`-visible launcher apps, each with a 3-state selector).
- **Timer detail:** default presets, sound/vibration for completion.

### 3.6 Theme selection
- Grid of theme cards rendered with the real pill component. Pro themes show a lock badge and "Pro" tag, and can be previewed (applied to the live overlay for 30 s) without purchase.
- Mode selector: System / Light / Dark / AMOLED.
- Accent: Dynamic (Android 12+) / 6 presets / Custom picker (Pro).
- Custom theme editor (Pro): 3 color pickers + style, contrast warning when < 4.5:1.

### 3.7 Privacy settings
- Plain-language summary card ("What Island can see", "What Island stores", "What leaves your phone: nothing").
- Toggles: hide content on lock screen, hide content while screen recording (API 35+, otherwise disabled with an explanation), local diagnostics.
- "View local diagnostics" (non-content event log) with an export-to-share-sheet option (user-initiated).
- "Revoke notification access" (deep link) and "Delete all data" (confirmation dialog).
- Links: privacy policy, open-source licenses.

### 3.8 About & subscription
- Version, changelog, support email, rate app, licenses.
- **Pro card:** benefits list, Annual and Lifetime price tiles (from Play Billing `ProductDetails`, never hard-coded), Restore purchases, and a "Manage subscription" deep link. Shown in MVP even if billing slips; in that case it reads "Coming soon — join the waitlist" (local opt-in only, used for the fake-door test).

---

## 4. Design system (`core:designsystem`)

### 4.1 Tokens

| Category | Tokens |
|---|---|
| Color (app) | Material 3 color scheme. Dynamic color on 31+. Brand fallback palette "Graphite" (neutral) with "Ion" accent `#5B8CFF` |
| Color (island) | `island.surface #0B0B0C`, `island.surfaceLight #F2F2F4`, `island.onSurfaceHigh 92 %`, `island.onSurfaceMedium 64 %`, `island.accent` (theme), `island.danger #FF5A5F`, `island.success #3DDC84` |
| Typography | Material 3 type scale. Island uses `labelLarge` (14 sp, medium) compact and `titleMedium`/`bodyMedium` expanded. **Tabular numbers** (`fontFeatureSettings = "tnum"`) for all times and percentages. Island text does **not** scale beyond 1.3× font scale (clamped) to keep the pill geometry, while the app scales fully |
| Spacing | 4-pt grid: `xxs 2, xs 4, s 8, m 12, l 16, xl 24, xxl 32` |
| Shape | `pill = 50 %`, `island.expanded = 28 dp`, `card = 20 dp`, `chip = 12 dp`, `art = 6/12 dp` |
| Elevation | Island: soft shadow 8 dp at 30 % alpha (optional). App: tonal elevation levels 0–3 |
| Motion | §2.4 spring tokens plus the duration scale |

### 4.2 Component inventory

| Component | Used in | Description |
|---|---|---|
| `IslandSurface` | Overlay, previews | Animated rounded container with cutout-aware lanes. Takes `IslandGeometry` + `Presentation` |
| `IslandLane` | Activity renderers | Start/end slot with enforced max width and fade-edge truncation |
| `IslandProgressBar` | Music, timer | Thin seek bar with optional drag |
| `IslandIconButton` | Expanded actions | 40 dp visual, 48 dp target, press spring |
| `EqualizerBars` | Music compact | 3–4 bars animated by an infinite transition, **paused when not visible** |
| `BatteryGlyph` / `ChargingSweep` | Battery | Vector glyph with fill level. Shader sweep |
| `RingProgress` | Timer, battery expanded | Canvas arc |
| `TimeText` | Timer, stopwatch, music | Tabular-figure text with minimal recomposition (only the changing digits) |
| `PhoneFramePreview` | App | Mock device frame hosting `IslandSurface` with demo state |
| `SettingsSection`, `SettingsSwitchRow`, `SettingsSliderRow`, `SettingsSegmentedRow`, `SettingsNavRow` | App | Consistent settings rows |
| `PermissionRow` | Onboarding, settings | State icon + title + why + action |
| `DisclosureScreen` | Onboarding | Policy-compliant prominent disclosure template |
| `ProBadge`, `PaywallCard`, `PriceTile` | App | Monetization |
| `ThemeCard` | Themes | Live mini island |

### 4.3 Component hierarchy (overlay)

```
OverlayRoot (ComposeView, CompositionLocals: IslandTokens, MotionPreset, Geometry)
└── IslandContainer (gesture detection, accessibility semantics)
    ├── IslandSurface(primary)                     ← animated bounds/shape
    │   └── when(presentation)
    │       ├── CompactLayout(start = renderer.compactStart, end = renderer.compactEnd)
    │       ├── MinimalLayout(renderer.minimal)
    │       └── ExpandedLayout(renderer.expanded)
    ├── IslandBubble(secondary)                     ← only in COMPACT_SPLIT
    └── QuickMenu (long-press, morphs from surface)
```

`ActivityRenderer` (contributed by features):

```kotlin
interface ActivityRenderer {
    val type: ActivityType
    @Composable fun Minimal(activity: LiveActivity)
    @Composable fun CompactStart(activity: LiveActivity)
    @Composable fun CompactEnd(activity: LiveActivity)
    @Composable fun Expanded(activity: LiveActivity, onAction: (ActionId) -> Unit)
}
```

### 4.4 App theming
- Light, dark, and dynamic color. Edge-to-edge is mandatory (target 36). Predictive back is supported.
- Layouts adapt via `WindowSizeClass`: compact (phone) is single column; medium/expanded (foldable inner, tablet) shows a two-pane settings layout with the preview pinned on the left. This needs no extra work beyond basic adaptive Scaffold. It is required because target 36 ignores orientation locks on large screens.

---

## 5. UX acceptance checklist
- [ ] Every island state renders correctly on a 360 dp-wide screen at 1.3× font scale.
- [ ] No text renders under the camera cutout on the reference devices (screenshot test per device profile).
- [ ] All interactive elements have a ≥ 48 dp touch target and a TalkBack label.
- [ ] Reduced-motion mode has no scale/overshoot animations.
- [ ] Light and dark app themes pass WCAG AA contrast for body text.
- [ ] Onboarding to a working island with music takes ≤ 90 s for a first-time user (usability test with 5 users).
