# Milestone 2: Signals & Event Collectors Design Specification

- **Date:** 2026-11-11
- **Status:** Approved Draft
- **Target Milestone:** Milestone 2 (Week 2 of 6-Week Roadmap)
- **Scope:** Normalized event model, permissions monitoring, DataStore settings, Room database v1, NotificationListenerService, media collector, battery collector, and timer domain.

---

## 1. Executive Summary & Goals

Milestone 2 bridges Android system signals into normalized domain events (`IslandEvent`) that feed the Island presentation engine. It introduces persistent configuration via DataStore and Room, validates permissions, and establishes the `NotificationListenerService` as the OS-level background anchor.

### Key Outcomes:
1. Define normalized event contracts (`IslandEvent`, `SystemEvent`, `EventSource`, `ActivityKey`, `PriorityHints`) in `:core:model`.
2. Implement `PermissionMonitor` in `:core:permissions` emitting `StateFlow<PermissionSnapshot>`.
3. Implement `:core:data` with typed `DataStore<IslandSettings>` (JSON serialization) and Room Database v1 (`AppNotificationRule` and `TimerEntity`).
4. Implement `:feature:notifications` with `IslandNotificationListenerService`, `NotificationEventBridge`, pure `NotificationFilter`, and `NotificationMapper`.
5. Implement `:feature:media` with `MediaSessionCollector` listening to `MediaSessionManager` active sessions and downsampling artwork.
6. Implement `:feature:battery` with `BatteryCollector` broadcasting charging and level alerts via `callbackFlow`.
7. Implement `:feature:timer` with `TimerRepository`, `TimerAlarmScheduler`, and `BootReceiver`.

---

## 2. Event Domain Architecture (`:core:model`)

Platform types (`StatusBarNotification`, `MediaController`, `Intent`) never enter domain logic. Collectors convert all platform data into immutable data classes at the edge.

### 2.1 Event Hierarchy
```kotlin
package io.selorax.island.model

public enum class EventSource {
    NOTIFICATION,
    MEDIA,
    BATTERY,
    TIMER,
    SYSTEM
}

public data class ActivityKey(public val raw: String)

public data class PriorityHints(
    public val priority: ActivityPriority = ActivityPriority.DEFAULT,
    public val isUrgent: Boolean = false
)

public sealed interface IslandEvent {
    public val key: ActivityKey
    public val source: EventSource
    public val timestamp: Long

    public data class Upsert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val hints: PriorityHints = PriorityHints()
    ) : IslandEvent

    public data class End(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val reason: String? = null
    ) : IslandEvent

    public data class Alert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val durationMs: Long
    ) : IslandEvent
}

public sealed interface SystemEvent {
    public data class ScreenState(public val isInteractive: Boolean, public val isLocked: Boolean) : SystemEvent
    public data class Orientation(public val isLandscape: Boolean) : SystemEvent
    public data class PermissionsChanged(public val hasOverlay: Boolean, public val hasNotificationAccess: Boolean) : SystemEvent
}
```

---

## 3. Permissions Monitoring (`:core:permissions`)

### 3.1 `PermissionSnapshot`
- `hasOverlayPermission`: Boolean (`Settings.canDrawOverlays(context)`).
- `hasNotificationAccess`: Boolean (checks if Island package is enabled in notification listener settings).
- `canScheduleExactAlarms`: Boolean (`AlarmManager.canScheduleExactAlarms` on API 31+).

### 3.2 `PermissionMonitor`
Exposes `val permissions: StateFlow<PermissionSnapshot>` and a `fun checkPermissions(): PermissionSnapshot` method to update state when the user returns from settings screens.

---

## 4. Local Persistence (`:core:data`)

### 4.1 DataStore Settings (`IslandSettings`)
- **Document Path:** `island_settings.json`
- **Serializer:** `kotlinx.serialization.json.Json` with `ReplaceFileCorruptionHandler` producing `IslandSettings()`.
- **Fields:**
  - `schemaVersion: Int = 1`
  - `islandEnabled: Boolean = true`
  - `anchor: String = "CUTOUT"`
  - `hideInLandscape: Boolean = true`
  - `onboardingCompleted: Boolean = false`
  - `notificationDisclosureAccepted: Boolean = false`

### 4.2 Room Database v1 (`IslandDatabase`)
- **`AppNotificationRule` Entity:**
  - `packageName: String` (Primary Key)
  - `enabled: Boolean = true`
  - `showPreview: Boolean = true`
  - `updatedAt: Long`
- **`TimerEntity` Entity:**
  - `id: String` (Primary Key)
  - `label: String`
  - `targetEpochMs: Long`
  - `originalDurationMs: Long`
  - `isRunning: Boolean`
  - `remainingPauseMs: Long?`
  - `createdAt: Long`
- **Strict Privacy Rule (ADR-07):** No message bodies, titles, contact names, or media information are stored in Room or DataStore.

---

## 5. Signal Collectors

### 5.1 Common Collector Interface
```kotlin
package io.selorax.island.common

import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow

public interface EventCollector {
    public val source: EventSource
    public val events: Flow<IslandEvent>
}
```

### 5.2 Notifications Subsystem (`:feature:notifications`)
- `IslandNotificationListenerService`: Android `NotificationListenerService` registered in manifest with `BIND_NOTIFICATION_LISTENER_SERVICE`.
- `NotificationSnapshot`: Lightweight DTO extracted immediately on the main thread:
  - `key: String`, `packageName: String`, `postTime: Long`, `title: String?`, `text: String?`, `isOngoing: Boolean`, `isGroupSummary: Boolean`, `category: String?`.
- `NotificationFilter`: Rejects Island's own package, group summaries, ongoing/foreground services, and media categories.
- `NotificationMapper`: Converts accepted snapshots to `IslandEvent.Upsert` with `IslandState.Compact` and `IslandState.Expanded`.
- `NotificationEventBridge`: Central channel/flow emitting notification events.

### 5.3 Media Session Collector (`:feature:media`)
- `MediaSessionCollector`:
  - Listens to `MediaSessionManager.OnActiveSessionsChangedListener`.
  - Attaches `MediaController.Callback` to active controllers.
  - Monitors `PlaybackState.STATE_PLAYING`, `STATE_PAUSED`, `STATE_STOPPED`.
  - Scales album artwork to ≤ 256 px.
  - Emits `IslandEvent.Upsert` with play status, track title, and artist, and `IslandEvent.End` on playback stop.

### 5.4 Battery Collector (`:feature:battery`)
- `BatteryCollector`:
  - Uses `callbackFlow` registering broadcast receiver for `ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`.
  - Emits `IslandEvent.Alert` on charge plugged ("Charging - XX%"), 100% full, and low battery (≤ 15%).
  - Debounces redundant updates via `distinctUntilChanged`.

### 5.5 Timer Domain & Alarms (`:feature:timer`)
- `TimerRepository`: Room-backed management of countdown timers.
- `TimerAlarmScheduler`:
  - Schedules alarm via `AlarmManager.setExactAndAllowWhileIdle` when permission is present.
- `BootReceiver`: Listens for `ACTION_BOOT_COMPLETED` to restore running timers from Room after reboot.

---

## 6. Definition of Done for Milestone 2

1. `:core:model` contains `IslandEvent`, `SystemEvent`, `EventSource`, and `ActivityKey` with unit tests.
2. `:core:permissions` contains `PermissionMonitor` with tests.
3. `:core:data` contains `DataStore<IslandSettings>` and Room v1 (`IslandDatabase`, `AppRuleDao`, `TimerDao`) with unit tests.
4. `:feature:notifications` filter and mapper unit tested with fixture data.
5. `:feature:media` collector compiles with media metadata normalization.
6. `:feature:battery` collector emits distinct charge state alerts.
7. `:feature:timer` repository and alarm scheduler tested.
8. `./gradlew test` passes 100% across all modules.
