# Implementation Guide (for Claude / AI-assisted and human contributors)

Version 0.1 · 2026-11-11 · Status: Draft for approval

> **Hard gate:** do not write application code until the user explicitly approves the plan in `docs/`. Once approved, follow this guide milestone by milestone. **Never skip a milestone's acceptance criteria.**

---

## 1. Ground rules

1. **Read before acting.** Before each milestone, re-read the relevant sections of TECHNICAL_ARCHITECTURE.md, ANDROID_COMPATIBILITY.md, and that week's DEVELOPMENT_ROADMAP.md section.
2. **Don't overwrite without looking.** The repo started as an IntelliJ Kotlin/JVM template. Converting it (Week 1, task 1.1) replaces `build.gradle.kts` and deletes `src/main/kotlin/Main.kt`. Do this only after `git init` and an initial commit of the template, so it's recoverable, and with user confirmation.
3. **Verify APIs at implementation time.** Anything marked **[Verify]** in the docs must be checked against current developer.android.com docs or a prototype before it's relied on. Record the result in the relevant doc.
4. **MVP scope is fixed.** Don't implement anything in PRODUCT_REQUIREMENTS §6 "Out of scope". Propose additions as backlog items.
5. **Simplest reliable implementation wins.** Don't add a library, module, or abstraction without a concrete need tied to a requirement ID.
6. **No backend, no analytics SDK, no Accessibility service, no `INTERNET` permission in MVP.**
7. **Update docs when reality differs.** If a prototype disproves an assumption, update the doc and the ADR in the same change.

---

## 2. Recommended implementation sequence

```
W1  build-logic → core:model → core:common → overlay(host, window, position) → [P1, P4, P5 prototypes]
W2  core:data → core:permissions → feature:notifications(listener, bridge, filter, mapper)
    → feature:media(collector) → feature:battery(collector) → feature:timer(domain, alarms, boot) → [P2, P3]
W3  engine(reducer → priority → lifecycle/deadlines → actor → processor) → overlay(renderer registry, gestures)
    → basic renderers → action handlers → consent gate → process-death rebuild → (Reliability FGS if decided)
W4  core:designsystem → final renderers + motion → feature:onboarding → feature:settings screens → Pro flags → P1 items
W5  device matrix → OEM flags → perf/leaks → release build → closed-track submission
W6  bug bash → compliance evidence → listing → (billing stretch) → rollout
```

**Why this order:** the overlay (highest technical risk) and the NLS anchor (highest platform risk) are proven before the engine is built on top of them. The engine is pure Kotlin and fast to build once inputs and outputs are stable.

---

## 3. Coding conventions

### 3.1 Kotlin
- Kotlin official code style (`kotlin.code.style=official` already set). ktlint + detekt with the default rule sets. A warning is a build failure in CI.
- `explicitApi()` in `core:model`, `core:common`, and `engine` (public API must be intentional).
- Immutability: `data class` + `val`. Collections exposed as `List`/`Map`, and `kotlinx.collections.immutable` (`ImmutableList`) for Compose-facing state.
- Mark Compose-facing models `@Immutable`/`@Stable` in `core:model`. Use the Compose runtime annotation artifact only (`androidx.compose.runtime:runtime-annotation`), which carries no UI dependency. **[Verify]** artifact availability; otherwise use a stability config file.
- No `!!` in production code. No `lateinit` outside Android framework entry points.
- Errors: domain errors as sealed results. Platform exceptions (`SecurityException`, `BadTokenException`, `DeadObjectException`) caught **at the edge** and converted to events/state.
- Naming: `XxxCollector`, `XxxMapper`, `XxxRenderer`, `XxxRepository`, `XxxDataSource`, `XxxUseCase` (only if it has real logic), `FakeXxx` for test doubles.

### 3.2 Coroutines
- Inject `DispatcherProvider`. Never hard-code `Dispatchers.IO` in classes.
- Scopes:
  - `ApplicationScope` (`SupervisorJob + Default`) for engine and repositories.
  - `OverlayHost.lifecycleScope` for UI.
  - Never `GlobalScope`.
- Collectors: `callbackFlow` + `awaitClose { unregister }` is mandatory. A unit test asserts unregistration.
- Main thread work from system callbacks must be < 1 ms: snapshot fields, `tryEmit`, return.
- `StateFlow` updates in the actor only. No other component calls `MutableStateFlow.value =` on engine state.

### 3.3 Compose
- State hoisting. Screens take `UiState` + lambdas. ViewModels expose `StateFlow<UiState>` collected with `collectAsStateWithLifecycle`.
- Defer state reads in animations (`Modifier.graphicsLayer { }`, lambda modifiers) to avoid recomposition per frame.
- No `remember { mutableStateOf }` for data that belongs to the engine.
- Infinite animations (equalizer, charging sweep) only while the island lifecycle is `RESUMED` and the activity is visible.
- Previews for every component in light/dark.

### 3.4 Android
- Context-registered receivers via `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`.
- All `PendingIntent`s we create are `FLAG_IMMUTABLE` unless mutability is required (document why).
- Version checks via `Build.VERSION.SDK_INT >= Build.VERSION_CODES.X`, wrapped in small `Compat` helpers in the owning module (e.g., `WindowCompat30`).
- Strings in resources. Plurals for counts. No hard-coded user-visible text.

### 3.5 Logging and privacy
- Only use the `Logger` facade. **Never log notification text, titles, media titles, or artwork.** Log `ActivityKey` and source only.
- Debug-only tooling (event inspector, state probe) lives in `src/debug` source sets and must not compile into release.

---

## 4. Architectural rules (enforced)

| Rule | Enforcement |
|---|---|
| `core:model`, `core:common`, `engine` have no Android dependencies | JVM library plugin. Build fails if `android.*` imported |
| No feature module depends on another feature module | Convention plugin check over `project.dependencies` + dependency-rule unit test (Konsist or a custom Gradle task) |
| `overlay` does not depend on `feature:*` | Same |
| Only `feature:notifications` may depend on `overlay`, and only on `OverlayHostController` | Konsist test on imports |
| `core:data` never references `ActivityContent.Notification` | Konsist test |
| Engine state mutations happen only inside `LiveActivityManager`'s actor loop | Code review + `MutableStateFlow` is `private` in the manager |
| Platform types (`StatusBarNotification`, `MediaController`, `Intent`) never appear in `core:model`/`engine` signatures | Konsist test |
| Every collector declares `requiredPermissions` and is started only via `EventProcessor` | Code review + test |
| No direct `WindowManager` calls outside `WindowController` | Konsist test |
| Forbidden manifest permissions | CI merged-manifest gate |

---

## 5. Module boundaries (quick reference)

```
app ──► feature:* , overlay , core:*
feature:{media,battery,timer} ──► engine , core:{model,common,data,designsystem}
feature:notifications ──► engine , core:* , overlay(OverlayHostController only)
feature:{settings,onboarding} ──► core:{model,common,data,permissions,designsystem}
overlay ──► engine , core:{model,common,data,designsystem}
engine ──► core:{model,common}
core:data, core:permissions ──► core:{model,common}
core:designsystem ──► core:model
core:common ──► core:model
```

Extension points that cross boundaries via Hilt multibinding in `app`:
- `Set<EventCollector>`
- `Set<ActivityRenderer>`
- `Set<ActivityActionHandler>`

---

## 6. Definition of Done (per task / PR)

- [ ] Linked to a requirement ID (FR-/NFR-/US-) or roadmap task number.
- [ ] Builds. ktlint/detekt/lint clean.
- [ ] Unit tests for new logic. Engine changes keep ≥ 90 % coverage.
- [ ] Integration/instrumented test, or a documented manual script, for system-boundary code.
- [ ] Screenshot tests updated for UI changes, goldens reviewed.
- [ ] No new permissions, or a new permission justified in ANDROID_COMPATIBILITY §3 with user approval.
- [ ] No notification/media content in logs, disk, or network (self-check against §3.5).
- [ ] No new polling loops. Any ticker is visibility-scoped.
- [ ] `WindowController` update count unchanged or justified.
- [ ] Docs updated if behavior or architecture changed (ADR updated if a decision changed).
- [ ] Manually verified on at least one physical device for overlay/collector changes.

**Definition of Done (milestone):** all of the week's P0 acceptance criteria in DEVELOPMENT_ROADMAP.md are checked with evidence (test names, screenshots, trace numbers), and the user has been shown the summary.

---

## 7. Milestone instructions

### M1 — Week 1: Foundation & overlay
1. `git init`. Commit the template as-is ("chore: initial IntelliJ template"). Ask the user before replacing the build files.
2. Create `gradle/libs.versions.toml`. Pick the **latest stable** versions of AGP, Kotlin (2.4.x), Compose BOM, Hilt, KSP, Room, DataStore, Lifecycle, Navigation Compose, Coroutines, kotlinx-serialization, Turbine, MockK, Robolectric, Roborazzi, LeakCanary. Record the chosen versions in the PR description. No alpha/beta unless a documented need exists.
3. `build-logic` convention plugins: compileSdk 37, minSdk 29, targetSdk 36, JVM 21, Compose enabled per module, R8 full mode for release.
4. Create modules per §5. Add `include(...)` to `settings.gradle.kts` (keep the existing foojay plugin and `rootProject.name`).
5. Implement `OverlayHost` + `WindowController` + `PositionCalculator`, and a debug button to show a static pill.
6. Run prototypes P1, P4, P5 per TESTING_STRATEGY §5.3. Write `docs/prototypes/week1-findings.md`.
7. **Stop and report findings to the user** before Week 2 if any prototype contradicts the architecture.

### M2 — Week 2: Collectors
1. `core:data` (DataStore + Room v1, exported schema), `core:permissions`.
2. NLS + bridge + filter + mapper. Keep the main-thread callback minimal.
3. Media, battery, and timer collectors, each as a `callbackFlow` with tests.
4. Prototypes P2 (start the soak on day 1 of the week; it needs time) and P3.
5. Decide Reliability mode (ADR-04) with the user based on P2 data. Update docs.

### M3 — Week 3: Engine
1. Write the reducer first as a pure function with exhaustive tests, then `PriorityPolicy`, then the deadline scheduler, then the actor wrapper, then `EventProcessor`.
2. Wire the overlay to `StateFlow<IslandState>`. Add the renderer registry with basic renderers.
3. Gestures → intents → action handlers.
4. Consent gate and process-death rebuild.
5. Measure NFR-01 latency with trace sections. Report it.

### M4 — Week 4: UI & customization
1. Tokens and components first, then renderers, then screens.
2. Live preview path: settings override flow → overlay, with no persistence per frame.
3. Onboarding disclosure text must match ANDROID_COMPATIBILITY §5 and C1. Get user sign-off on the copy.
4. Pro flags via `EntitlementProvider` (no billing yet).

### M5 — Week 5: Compatibility & performance
1. Execute the device matrix. File issues with device/OS/build info.
2. Performance measurements against the NFR table. Fix regressions before new features.
3. Release build configuration. Submit to closed testing (requires the user's Play Console access — **ask the user to do the upload**. Never handle their credentials).

### M6 — Week 6: QA & store
1. Bug bash triage, then compliance evidence (C1–C17), then listing assets (with the user).
2. Billing only if all P0 items are done and the user approves the stretch.
3. Prepare the staged rollout plan. The user performs the production release.

---

## 8. Things that require explicit user approval
- Replacing or deleting any existing file in the repo (beyond `docs/`).
- Adding any permission not listed in ANDROID_COMPATIBILITY §3, or enabling Reliability mode (`specialUse` FGS).
- Adding any dependency that makes network calls, or any SDK (analytics, crash reporting, ads).
- Changing minSdk/targetSdk, or an ADR decision.
- Anything involving the Play Console, signing keys, or publishing.
- Expanding MVP scope.
