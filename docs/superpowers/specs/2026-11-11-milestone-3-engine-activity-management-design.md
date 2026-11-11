# Milestone 3: Engine & Activity Management Design Specification

- **Date:** 2026-11-11
- **Status:** Proposed (Design Approved)
- **Target:** Milestone 3 (Week 3 of Development Roadmap)
- **Branch:** `feature/milestone-1-foundation-overlay` (no auto-commit per user constraint)

---

## 1. Problem Statement & Scope

Milestone 2 completed the signal collection layer, producing normalized streams of `IslandEvent` (`Upsert`, `End`, `Alert`, `Batch`) from notifications, media playback, battery events, and countdown timers.

Milestone 3 builds the core brain of the system:
1. **Collector Orchestration (`EventProcessor`)**: Gating collector streams on settings and runtime permissions, deduplicating bursts (500ms), coalescing media updates (50ms), and enforcing rate limits (max 1 notification alert per app per 3s).
2. **Deterministic State Management (`LiveActivityManager` Actor)**: An actor loop consuming an ordered `Channel<Command>(capacity = 128)`, executing a pure state reducer, and driving a single unified deadline scheduler `Job` (no per-activity coroutines).
3. **Priority Ranking (`PriorityPolicy`)**: A table-driven, pure mathematical ranking model that handles preemption guards (2.5s minimum display time), urgency overrides, and split pill slot assignments.
4. **Basic UI Presentation & Gestures (`:overlay`)**: Connecting `StateFlow<IslandState>` to Compose renderers for all 4 signal types (Notification, Media, Battery, Timer), with `GestureHandler` translating touch inputs into `UserIntent`.

---

## 2. Module Responsibilities & Dependency Boundaries

Per `TECHNICAL_ARCHITECTURE.md` §4:
- **`:engine` (Pure JVM Library)**:
  - Depends only on `:core:model` and `:core:common`.
  - **Forbidden:** Android SDK dependencies (`android.*`), Room/DataStore, or Compose UI.
  - Allows 100% test coverage with coroutines virtual time testing (`runTest`, `StandardTestDispatcher`).
- **`:overlay` (Android Library with Compose)**:
  - Depends on `:engine`, `:core:model`, `:core:common`, `:core:designsystem`, `:core:data`.
  - Implements `IslandRenderer`, `GestureHandler`, and host-level event wiring.
- **`:feature:*`**:
  - Expose `EventCollector` implementations and action handlers for user intents.

---

## 3. Detailed Component Architecture

### 3.1 EventProcessor (Collector Orchestrator)

```kotlin
public class EventProcessor(
    private val collectors: Set<EventCollector>,
    private val settingsRepository: SettingsRepository,
    private val permissionMonitor: PermissionMonitor,
    private val dispatcherProvider: DispatcherProvider
) {
    public fun process(): Flow<IslandEvent>
}
```

**Pipeline Stages:**
1. **Permission & Settings Gating (`flatMapLatest`):**
   - Combines `settingsRepository.settings` and `permissionMonitor.permissions`.
   - Starts only collectors whose source is enabled in settings and whose required permissions are granted. Disabling a feature automatically unregisters its system listeners.
2. **Deduplication:**
   - Drops duplicate events with identical `(key, contentHash)` arriving within 500ms.
3. **Media Coalescing:**
   - Groups metadata and playback state updates within a 50ms window for the same media key to prevent artwork/title flicker.
4. **Notification Rate Limiting:**
   - Enforces a leaky-bucket or sliding-window rate limit: at most 1 alert per package name every 3.0 seconds.
5. **Consent Gate:**
   - Filters out notification events if `settings.notificationDisclosureAccepted` is false.

---

### 3.2 LiveActivityManager (Actor Model)

```kotlin
public sealed interface EngineCommand {
    public data class ApplyEvent(val event: IslandEvent) : EngineCommand
    public data class HandleIntent(val intent: UserIntent) : EngineCommand
    public data class Tick(val currentEpochMs: Long) : EngineCommand
    public data class SetSystemState(val event: SystemEvent) : EngineCommand
}

public sealed interface EngineEffect {
    public data class TriggerHaptic(val effectType: HapticType) : EngineEffect
    public data class LaunchIntent(val action: ActivityAction) : EngineEffect
}
```

**Actor Loop & Deadlines:**
- `LiveActivityManager` maintains:
  - `_state: MutableStateFlow<IslandState>` (exposing public `state: StateFlow<IslandState>`).
  - `_effects: Channel<EngineEffect>` (exposing public `effects: Flow<EngineEffect>`).
  - `commandChannel: Channel<EngineCommand>(capacity = 128)`.
  - `deadlineJob: Job?` (a single coroutine controlling the next earliest event).
- **Single Deadline Scheduler:**
  - After each command reduces state, the manager inspects all active activities:
    - Minimum display deadlines (e.g. 2500ms from appearance).
    - Auto-collapse deadlines (e.g. 8000ms after expanding).
    - Transient alert expiry deadlines (e.g. 3000ms for battery plug-in, 5000ms for notification alert).
    - Media pause linger (30000ms after pause before dismissal).
  - It schedules a single delay to the earliest deadline, emitting `EngineCommand.Tick` when reached.

---

### 3.3 PriorityPolicy & ActivityRegistry

**Activity Registry:**
- Retains up to 20 recent notification activities and active persistent activities (media, timers).
- Removes expired or dismissed items.

**Priority Equation:**
```text
EffectivePriority = basePriority(type, userOrder) + urgencyBoost + alertBoost + interactionBoost - staleness
```
- **Base Priorities:**
  - Timer: 90
  - Battery Alert: 85
  - Media: 70
  - Notification: 50
- **Boosts & Penalties:**
  - Urgency Boost: `+60` (Timer finished, battery ≤ 5%)
  - Transient Alert Boost: `+40` for initial display duration (3-5s)
  - Interaction Boost: `+30` while expanded by the user
  - Staleness: `-1` per 10s since last update (clamped to max `-20`)
- **Preemption Guard:**
  - If an activity has been visible for `< 2500ms`, a newcomer cannot preempt it as primary unless the newcomer's priority delta is `≥ +60` (urgent override).
  - If preemption is blocked and split mode is enabled, the newcomer displays in the secondary slot or queues.

---

### 3.4 Presentation Mapping & Overlay Integration

`IslandState` contains:
```kotlin
public data class IslandState(
    public val visibility: IslandVisibility,
    public val presentation: IslandPresentation,
    public val primary: LiveActivity?,
    public val secondary: LiveActivity?,
    public val queuedCount: Int
)
```
- **Visibilities:**
  - `Visible`: Normal operation.
  - `Hidden(reason)`: Suppressed during landscape, fullscreen games/video, device locked, or snoozed.
- **Presentations:**
  - `Minimal`: Icon-only or dot when secondary activity is active without split pill.
  - `Compact`: Default pill anchored to cutout.
  - `Split`: Primary pill on cutout + secondary bubble for concurrent persistent activities (e.g., Timer + Spotify).
  - `Expanded`: Large card displaying full controls, seek bars, or action buttons.

---

### 3.5 GestureHandler & UserIntents

In `:overlay`:
- **Tap**:
  - When Compact/Split: expands the tapped activity.
  - When Expanded: executes the primary action or launches the target app.
- **Long Press**:
  - Quick actions modal or direct settings shortcut.
- **Swipe Horizontal (Left/Right)**:
  - Dismisses transient notifications or skips media track.
- **Outside Tap (`FLAG_WATCH_OUTSIDE_TOUCH`)**:
  - Immediately collapses Expanded state back to Compact.

---

## 4. Verification & Testing Strategy

1. **Unit Testing (`:engine`)**:
   - `EventProcessorTest`: virtual time tests verifying 500ms deduplication, 50ms media coalesce, and rate-limiting.
   - `PriorityPolicyTest`: comprehensive table-driven tests checking ranking permutations, preemption guard enforcement, and urgency overrides.
   - `LiveActivityManagerTest`: actor testing with `StandardTestDispatcher` and `advanceTimeBy()`, validating auto-collapse at 8s, transient expiry at 3s, and pause linger at 30s.
2. **Integration Testing (`:overlay`)**:
   - `GestureHandlerTest`: verifying touch events map to expected `UserIntent` commands.
   - `IslandRendererTest`: snapshotting composition outputs for all 4 activity types across Compact and Expanded states.
3. **Full Build Check**:
   - `./gradlew test` across all 14 modules verifying 0 regressions.

---

## 5. Non-Functional & Safety Constraints

- **ADR-07 Privacy Compliance**: No raw notification bodies or contact names are persisted to disk or sent over any network.
- **No Automated Commits**: All changes stay in the working directory on branch `feature/milestone-1-foundation-overlay`.
