# Island — Android Live Activities: Planning Documentation

> Status: **PLANNING — awaiting approval.** No application code is to be written until the plan is explicitly approved.
> Date of plan: 2026-11-11. Working product name: **"Island"** (placeholder — see trademark note below).

## Document index

| # | Document | Purpose |
|---|----------|---------|
| 1 | [PRODUCT_REQUIREMENTS.md](PRODUCT_REQUIREMENTS.md) | Vision, users, user stories, FR/NFR, MVP scope, monetization |
| 2 | [TECHNICAL_ARCHITECTURE.md](TECHNICAL_ARCHITECTURE.md) | Diagrams, modules, event flow, lifecycle, ADRs, **architecture review** |
| 3 | [ANDROID_COMPATIBILITY.md](ANDROID_COMPATIBILITY.md) | API matrix, permissions, background limits, OEM behavior, Play compliance |
| 4 | [DATABASE_DESIGN.md](DATABASE_DESIGN.md) | Room entities, DataStore schema, data lifecycle, privacy |
| 5 | [UI_UX_SPECIFICATION.md](UI_UX_SPECIFICATION.md) | Screens, island states, gestures, motion, design system |
| 6 | [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md) | Six-week MVP plan, milestones, critical path |
| 7 | [TESTING_STRATEGY.md](TESTING_STRATEGY.md) | Unit / integration / UI / device / performance testing |
| 8 | [CLAUDE_IMPLEMENTATION_GUIDE.md](CLAUDE_IMPLEMENTATION_GUIDE.md) | Implementation order, conventions, rules, definition of done |

## Repository inspection (2026-11-11)

The working directory `/Volumes/work/personal/dynamic-island` contains an **IntelliJ "Kotlin/JVM" console template**, not an Android project:

| File | Content | Implication |
|------|---------|-------------|
| `build.gradle.kts` | `kotlin("jvm") 2.4.20`, `jvmToolchain(26)`, JUnit Platform | Must be replaced by an Android multi-module build. JDK 26 toolchain is **not** a supported AGP toolchain target — use JDK 21 for the Android build. |
| `settings.gradle.kts` | foojay resolver, `rootProject.name = "dynamic-island"` | Reusable; module `include(...)`s will be added. |
| `gradle/wrapper` | Gradle **9.6.0** | Keep, provided the chosen stable AGP supports it (verify on day 1). |
| `src/main/kotlin/Main.kt` | Template "Hello World" | Delete (with approval) when the Android `app` module is created. |
| `.idea/`, `.gradle/` | IDE state | Untouched. |
| Git | **Not a git repository** | `git init` is the first step of Week 1 (with approval). |

No files were modified. Only `docs/` was created.

## Key decisions at a glance (details in TECHNICAL_ARCHITECTURE.md §10)

| ID | Decision |
|----|----------|
| ADR-01 | Modular monolith, ~14 Gradle modules, feature modules are **vertical slices** (collector + mapper + renderer per activity type) |
| ADR-02 | `minSdk 29`, `targetSdk 36` (Play requirement since 2026-08-31), `compileSdk 37`; re-evaluate `minSdk 31` after Week 5 data |
| ADR-03 | Single overlay window (`TYPE_APPLICATION_OVERLAY`) rendering Compose; window resized at most **twice per transition** |
| ADR-04 | Process kept alive by the system-bound `NotificationListenerService` + visible overlay; **no always-on foreground service by default**; optional `specialUse` FGS "Reliability mode" as OEM fallback (prototype-gated) |
| ADR-05 | Single-writer **actor** (`LiveActivityManager`) consuming a bounded `Channel` of commands, publishing `StateFlow<IslandState>` |
| ADR-06 | Hilt for DI (`@AndroidEntryPoint` services, `@IntoSet` renderer/collector multibinding) |
| ADR-07 | **No AccessibilityService. No backend. No analytics SDK in MVP.** Notification content never leaves the device and is never persisted |
| ADR-08 | Typed DataStore for settings; Room only for per-app rules, timers, custom themes |
| ADR-09 | Play Billing for Pro, but **outside the MVP critical path** (Week 6 stretch or v1.1) |

## Top risks requiring early prototypes (Week 1)

| ID | Risk | Why it matters |
|----|------|----------------|
| P1 | Overlay placement around the camera cutout and **z-order under the status bar** | It's the core visual concept. `TYPE_APPLICATION_OVERLAY` sits below the status bar and notification shade by design |
| P2 | Process survival without an FGS on Xiaomi/OnePlus/Realme/Samsung | Determines whether ADR-04 holds or a `specialUse` FGS becomes mandatory |
| P3 | Launching a notification's `contentIntent` from the overlay (BAL rules on Android 14–17) | "Tap to open" is a core interaction |
| P4 | Touch pass-through and window resize cost | A badly sized overlay blocks touches on other apps, or janks |
| P5 | Full-screen / immersive detection without Accessibility | The island must not cover video or games |

## Trademark / naming note

"Dynamic Island" and "Live Activities" are Apple marketing terms. **Do not** use them in the app name, icon, or Play listing title. Google Play's metadata and impersonation policies, and trademark law, both apply. Internally we use *Island*, *Live Activity*, and *Activity* as domain terms only.

## Open questions for the product owner

1. Final product name and brand.
2. Solo developer or team? The six-week plan assumes **one senior Android engineer full-time plus part-time design/QA**.
3. Should crash reporting be in v1 (opt-in), or Play Vitals only? The default here is Play Vitals only.
4. Pricing region focus: global, or specific markets first?
5. Is a physical device lab available (see ANDROID_COMPATIBILITY.md §7 for the minimum set)?
