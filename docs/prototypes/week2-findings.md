# Week 2 Prototype Findings & Milestone 2 Verification Report

- **Date:** 2026-11-11
- **Status:** Complete & Verified
- **Milestone:** Milestone 2 (Signals & Event Collectors)

---

## 1. Prototype Findings

### Prototype P2: Notification Ingestion & Filtering (ADR-07)
- **Objective:** Stream incoming notifications safely into normalized `IslandEvent` signals while strictly respecting privacy and filtering system noise.
- **Implementation:**
  - `NotificationFilter` rejects notifications from Island itself, group summaries, ongoing/foreground service indicators, and low-priority system noise.
  - `NotificationMapper` translates raw `NotificationSnapshot` into `IslandEvent.Upsert` (with compact text truncated to 12 chars and leading package icon) and `IslandEvent.End` when dismissed.
  - `IslandNotificationListener` bridges `NotificationListenerService` callbacks into `NotificationEventBridge` (implementing `EventCollector`).
- **Privacy & Security (ADR-07):** Notification bodies, contact names, and artwork bitmaps are processed strictly in-memory and are never written to disk, SQLite, DataStore, or sent to any network endpoint.
- **Verification Evidence:**
  - `NotificationFilterTest`: Passes all rejection rules (own package, group summaries) and accepts valid incoming messages.
  - `NotificationEventBridge` and `IslandNotificationListener`: Compiles with `BIND_NOTIFICATION_LISTENER_SERVICE` declared in AndroidManifest.

---

### Prototype P3: Media Session Playback Tracking
- **Objective:** Track active media playback sessions and produce unified `IslandEvent` stream for music and podcast playback.
- **Implementation:**
  - `MediaSessionCollector` implements `EventCollector` for `EventSource.MEDIA`.
  - Emits `IslandEvent.Upsert` with `IslandSlot.Icon("music")` and title preview when playback starts.
  - Emits `IslandEvent.End` with reason `playback_paused` when playback stops.
  - Uses `MutableSharedFlow(extraBufferCapacity = 32)` to guarantee non-blocking emission without dropping critical state changes.
- **Verification Evidence:**
  - `MediaSessionCollectorTest`: Verified with Turbine testing library on both playback start (`Upsert`) and pause/stop (`End`).

---

### System Signals: Battery & Timer Event Collectors
- **Battery Collector:**
  - `BatteryCollector` monitors charging status and low battery thresholds.
  - Emits transient `IslandEvent.Alert` on charger plug-in (3000ms duration), unplug (2000ms duration), and critical low battery ≤ 15% (4000ms duration).
  - Verified with `BatteryCollectorTest` using Turbine.
- **Timer Collector:**
  - `TimerCollector` tracks countdown timers and formats remaining time into `MM:SS` display strings.
  - Emits `IslandEvent.Upsert` for ticks and `IslandEvent.End` with reason `timer_completed`.
  - `BootReceiver` declared with `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM` to ensure timers can be restored upon device reboot.
  - Verified with `TimerCollectorTest` using Turbine.

---

### Data Persistence: DataStore & Room Database
- **Settings DataStore:**
  - `IslandSettings` and `IslandSettingsSerializer` provide typed Kotlinx Serialization settings persistence.
  - Corruption handling verified: malformed or blank data falls back safely to default settings with `schemaVersion = 1`.
  - `SettingsRepository` exposes immutable `Flow<IslandSettings>` and granular update operations.
  - Verified with `IslandSettingsSerializerTest`.
- **Room v1 Database:**
  - `AppNotificationRule` and `TimerEntity` entities defined in `IslandDatabase`.
  - DAOs (`AppRuleDao`, `TimerDao`) compiled cleanly via KSP.
  - Zero sensitive notification payload data is stored in the database.

---

## 2. Verification Summary

| Component | Status | Evidence |
|---|---|---|
| Signal Domain Models (`:core:model`) | Passed | `IslandEvent`, `SystemEvent`, `EventSource`, `PriorityHints`, `ActivityKey` unit tested |
| Event Collector Interface (`:core:common`) | Passed | Clean `EventCollector` contract with `Flow<IslandEvent>` |
| Permissions Engine (`:core:permissions`) | Passed | `PermissionSnapshot` and `PermissionMonitor` unit tested with MockK |
| Settings DataStore (`:core:data`) | Passed | `IslandSettingsSerializerTest` round-trip and corruption tests pass |
| Room v1 Database (`:core:data`) | Passed | KSP code generation and DAO compilation passed |
| Notification Collector (`:feature:notifications`) | Passed | `NotificationFilterTest` passes, `IslandNotificationListener` compiled with manifest |
| Media Collector (`:feature:media`) | Passed | `MediaSessionCollectorTest` passes (Turbine flow test) |
| Battery Collector (`:feature:battery`) | Passed | `BatteryCollectorTest` passes (Turbine flow test) |
| Timer Collector (`:feature:timer`) | Passed | `TimerCollectorTest` passes, `BootReceiver` declared in manifest |
| Full Test Suite (`./gradlew test`) | Passed | 339 actionable tasks executed/up-to-date, 0 failures |
