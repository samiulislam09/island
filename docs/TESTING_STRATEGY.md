# Testing Strategy — Island

Version 0.1 · 2026-11-11 · Status: Draft for approval

---

## 1. Test pyramid and tooling

| Layer | Share | Runs on | Tools |
|---|---|---|---|
| Unit (pure Kotlin) | ~70 % | JVM, every commit | JUnit 5 (JVM modules) / JUnit 4 (Android modules, via Robolectric), **MockK** (sparingly — prefer fakes), **kotlinx-coroutines-test** (`runTest`, `TestScope`, virtual time), **Turbine** (Flow assertions), Truth or kotlin.test assertions |
| Android-JVM (Robolectric) | ~10 % | JVM, every commit | Robolectric for receivers, DataStore, PackageManager fallbacks |
| Screenshot | ~5 % | JVM, every commit | **Roborazzi** (Robolectric-native screenshot tests) for renderers and screens |
| Instrumented | ~10 % | Emulator (CI nightly) + devices | AndroidX Test, Compose UI Test, UI Automator (system settings, notification posting), Room `MigrationTestHelper` |
| Performance | — | Physical devices, per RC | **Macrobenchmark**, **Baseline Profiles**, Perfetto, `dumpsys gfxinfo/meminfo/batterystats`, Battery Historian, LeakCanary (debug only) |
| Manual / exploratory | ~5 % | Device matrix, per RC | Scripted checklists (§5) |

**Rules:**
- Engine and model tests must not use Robolectric or Android classes. If a test needs Android, the code is in the wrong module.
- Use **fakes** for our own interfaces (`FakeClock`, `FakeCollector`, `FakeSettingsRepository`). Use MockK only for Android/platform types that can't be faked cheaply.
- No `Thread.sleep`. All time is virtual (`TestScope.advanceTimeBy`), injected via `Clock`/`TimeSource`.

---

## 2. Unit testing

### 2.1 Engine (`engine`) — coverage target ≥ 90 % lines, 100 % of reducer branches
| Area | Representative cases |
|---|---|
| Reducer | Upsert new → Pending→Active. Upsert same key → update in place. End → Ending→Removed. Alert with TTL. Dismiss. Snooze. Unknown id intents ignored |
| PriorityPolicy | Table-driven: user order, urgency boost, transient boost, interaction boost, staleness. Ties broken by recency, then id |
| Preemption | Min display respected. Urgent overrides. Expanded never preempted (**property-based test**, random event sequences, invariant check) |
| Expiry | Timed counts from first visible. Queued max age 30 s. Persistent linger 30 s after pause. Auto-collapse 8 s |
| Deadline scheduler | Only one pending timer job. Rescheduled on earlier deadline. Cancelled on empty state |
| Visibility | Landscape, immersive, snooze, screen off, missing SAW → `HIDDEN(reason)` precedence |
| Dedupe/coalesce | Same content within 500 ms dropped. Media 50 ms coalescing. Per-app rate limit |
| Collector orchestration | Enabling/disabling a source starts/stops its flow (assert `awaitClose` called). Permission revoke stops it |
| Invariants (checked after every reducer step in tests) | ≤ 1 primary, ≤ 1 secondary. Secondary is persistent. No `Removed` items in registry. Notification count ≤ 20 |

### 2.2 Features
| Module | Cases |
|---|---|
| notifications | Filter matrix: own pkg, group summary, ongoing/FGS, media `CATEGORY_TRANSPORT`, low importance, DND mismatch, `VISIBILITY_SECRET`. Mapper redaction per rule. Label fallback chain |
| media | Playing-session selection with 0/1/many sessions. Pause linger. Capability bits → buttons. Position extrapolation (speed 1.0/1.5/0, negative/unknown duration). Artwork downscale dimensions |
| battery | Distinct emission on (level, plugged, status) only. Low once per cycle; reset on charge. Full once per charge |
| timer | Remaining-time math. Pause/resume accumulation. Boot-count mismatch → wall-clock path. Exact vs inexact scheduler selection |

### 2.3 Data
- DataStore serializer: defaults, round trip, unknown fields ignored, corrupted file → defaults.
- Schema migrations: `IslandSettings.schemaVersion` migration functions.
- Room DAOs (Robolectric or instrumented in-memory DB). Migration tests from every exported schema.

---

## 3. Integration testing

| Suite | Scope | Where |
|---|---|---|
| Engine pipeline | Fake collectors → `EventProcessor` → `LiveActivityManager` → `IslandState` (Turbine), real `PriorityPolicy`, `FakeSettingsRepository` | JVM |
| Notification end-to-end | Real NLS on emulator: `adb shell cmd notification post -S bigtext -t "Title" tag "Body"` → assert island state via a debug-only `IslandStateProbe` (bound service exposing state as text, **debug variant only**) | Instrumented |
| Media end-to-end | Test app module (`testing/mediaapp`, debug only) publishes a `MediaSession` with controllable metadata/state → assert state and that transport commands reach it | Instrumented |
| Battery | `adb shell dumpsys battery set level 14`, `set ac 1`, `unplug`, `reset` → assert alerts | Instrumented / manual |
| Timer + process death | Start timer → `adb shell am kill` → wait → assert alarm fires and state restored. Reboot variant on device | Device |
| Permissions | `adb shell appops set <pkg> SYSTEM_ALERT_WINDOW deny/allow`, `cmd notification disallow_listener/allow_listener` → assert degradation and recovery | Instrumented |

---

## 4. UI testing

- **Compose UI tests** for every app screen: state → rendered semantics. Permission row states. Onboarding navigation (including "Not now" paths). Settings writes reach the repository fake.
- **Screenshot tests (Roborazzi):** each `ActivityRenderer` × {Minimal, Compact, Compact split, Expanded} × {light pill, dark pill} × {font 1.0, 1.3} × {cutout center, no cutout}. App screens in light/dark. Golden images reviewed in PRs.
- **Overlay instrumentation:** attach host with a fake state on an emulator. Assert window params (type, flags, size == bounds + slop) via a `WindowController` test hook. Perform a UI Automator tap outside bounds and assert it reaches a test activity underneath.
- **Accessibility:** Compose `assertContentDescription`/touch-target checks. Play pre-launch accessibility report. TalkBack manual script.

---

## 5. Device compatibility testing

### 5.1 Matrix
| Dimension | Values |
|---|---|
| OS (emulator) | API 29, 30, 31, 33, 34, 35, 36, 37 |
| OEM (physical) | Pixel, Samsung (A + S/Z), Xiaomi HyperOS, OnePlus or Realme (ColorOS), Motorola |
| Cutouts | Center punch-hole, corner punch-hole, notch (emulator "Tall cutout"/"Double cutout" display cutout overlays), none, waterfall |
| Densities/sizes | 320–560 dpi; 360–480 dp width; foldable inner (sw ≥ 600) |
| Modes | Portrait, landscape, split screen, foldable fold/unfold, battery saver on, dark mode, font 1.3× / 2.0×, display size largest, gesture vs 3-button nav |

Emulators can simulate cutouts via Developer options → "Display cutout". Use this for CI screenshot sanity on API 29+.

### 5.2 Per-device smoke script (≈ 20 min, every RC)
1. Fresh install → onboarding → grant all → island appears.
2. Play music (Spotify + YouTube Music) → compact/expanded/controls/seek.
3. Charge connect/disconnect → animation. `dumpsys battery set level 14` → low alert.
4. Send messages (WhatsApp/Telegram/SMS) → preview, expand, open, dismiss. Lock → redacted.
5. Start 2-min timer → kill app from recents → timer completes.
6. Rotate, full-screen YouTube, game → island hides. Back → returns.
7. Pull notification shade → island covered (expected). Release → no glitches.
8. Leave the screen off 1 h → wake → island alive? (record). Repeat over 8 h overnight on OEM devices (P2 protocol).
9. Revoke overlay permission → app shows degraded state. Re-grant → recovers.
10. Reboot → unlock → island returns without opening the app (record time).

### 5.3 Prototype protocols (Week 1–2)
- **P1:** screenshot the pill with the status bar visible, shade half-open, lock screen, AOD. Record icon overlap per device.
- **P2:** 24 h soak per OEM. Log listener connect/disconnect and process start timestamps to local diagnostics. Report median time-to-rebind and number of kills.
- **P3:** top-20 apps × {unlocked, locked, from another app's full screen}. Record success/failure per send strategy.
- **P4:** 50 scripted expand/collapse cycles with `adb shell dumpsys gfxinfo <pkg> framestats` and a Perfetto trace. Count `updateViewLayout` calls.
- **P5:** YouTube full-screen, Netflix, 2 games, camera app. Record whether the inset heuristic detects each.

### 5.4 Expected-difference documentation
Each RC updates a compatibility table (in-repo `docs/reports/compat-<version>.md`) of features × devices with ✅/⚠️/❌ and notes. ⚠️/❌ items must map to either in-app guidance or a known-issue entry.

---

## 6. Performance testing

| NFR | Method | Tool | Pass criterion |
|---|---|---|---|
| NFR-01 Latency | Custom `Trace.beginAsyncSection("island:event:<key>")` at the collector callback, end at the first frame after the state change (`Choreographer` callback in `OverlayHost`) | Perfetto, 100 events | p50 ≤ 50 ms, p95 ≤ 120 ms |
| NFR-02 Jank | 50 transitions scripted via debug intent | `gfxinfo framestats`, Perfetto FrameTimeline | ≤ 1 % janky (Pixel 7a-class), ≤ 3 % (low-end) |
| NFR-03 Idle CPU | Island visible, nothing changing, screen on 30 min / off 2 h | `dumpsys cpuinfo`, Perfetto CPU sched, `batterystats` wakeups | 0 app-attributable wakeups when idle; < 0.5 % avg CPU |
| NFR-04 Memory | Overlay visible; 4 h soak with music + notifications | `dumpsys meminfo` every 15 min | PSS ≤ 80 MB, heap growth ≤ 5 MB |
| NFR-05 Battery | 24 h typical use on 2 devices, with/without app (A/B days) | Battery Historian, Settings battery usage, Play Vitals (post-launch) | ≤ 1 %/24 h attributed |
| NFR-06 Startup | Cold start `MainActivity` | Macrobenchmark `StartupTimingMetric`, with Baseline Profile | TTID ≤ 600 ms (median of 10) |
| NFR-07 Recovery | `am kill` → measure time until island visible | Logcat timestamps + diagnostics | ≤ 3 s after rebind |

### 6.1 Specific anti-pattern tests
| Concern | Test |
|---|---|
| Unnecessary polling | Static check: no `while(true)`/`delay` loops outside approved tickers (`MediaProgressTicker`, `TimerTicker`). Runtime: Perfetto shows no periodic app wakeups when collapsed and idle |
| Memory leaks | LeakCanary in debug. Instrumented test attaches/detaches the overlay 100× and asserts heap returns within 2 MB of baseline after GC. Collector `awaitClose` unregistration verified by unit tests |
| Duplicate events | Integration test: notification reposts/ranking updates and media metadata bursts → exact expected event counts |
| Excessive foreground services | Instrumented: `ActivityManager.getRunningServices` (own app) shows 0 FGS when Reliability mode is off |
| Repeated WindowManager updates | Debug counter in `WindowController`. Test asserts ≤ 2 `updateViewLayout` per transition and 0 while idle |
| Unnecessary DB operations | Room `QueryCallback` in debug counting queries. Test: 100 notifications → 0 DB queries |
| Recomposition churn | Compose compiler metrics + Layout Inspector recomposition counts. `TimeText` recomposes once per second in compact |

---

## 7. Privacy testing
- **Grep gate in CI:** release build fails if `Log.` calls exist outside the logger facade, or if `INTERNET`, `QUERY_ALL_PACKAGES`, `USE_EXACT_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, or `BIND_ACCESSIBILITY_SERVICE` appear in the merged release manifest.
- **Storage audit test:** after a scripted session with 50 unique notification texts, pull `/data/data/<pkg>` (debuggable build) and grep for any of the texts → must find **0 matches**.
- **Network audit:** run the session behind a proxy/`NetworkStatsManager` → 0 bytes sent by the app UID.
- **Consent gate test:** grant notification access via adb without onboarding consent → no notification activities are produced.

---

## 8. CI pipeline

| Stage | Trigger | Content | Budget |
|---|---|---|---|
| PR | Every push | Build, detekt/ktlint, Android lint, unit + Robolectric + Roborazzi verify, dependency-rule test, manifest gate | ≤ 10 min |
| Nightly | Schedule | Instrumented tests on emulators API 29, 33, 36, 37 (Gradle Managed Devices), migration tests | ≤ 45 min |
| RC | Tag | Macrobenchmark on physical device, full device smoke script (manual), performance report | Manual sign-off |

## 9. Definition of "tested"
A feature is tested when:
- unit tests cover its logic,
- an integration test covers its system boundary (or a documented manual script if not automatable),
- screenshot tests exist for any UI,
- and it passed the per-device smoke on Pixel + Samsung + one aggressive OEM.
