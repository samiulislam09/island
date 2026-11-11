# Week 3 Prototype Findings & Milestone 3 Verification Report

- **Date:** 2026-11-11
- **Status:** Complete & Verified
- **Milestone:** Milestone 3 (Event Processing & Activity Management)

---

## 1. Architecture & Engine Findings

### 1.1 Pure JVM Engine Design
- **Objective:** Ensure the core decision-making brain (`:engine`) has zero Android SDK coupling, enabling high-speed deterministic unit testing with virtual coroutine time.
- **Implementation:**
  - `:engine` configured with `island.jvm.library` and `island.test` plugins.
  - Depends solely on `:core:model` and `:core:common`.
  - All state transitions and deadline ticks run via standard Kotlin coroutines.
- **Verification Evidence:**
  - All tests in `:engine` execute in JVM test suites using `StandardTestDispatcher` and `runTest` without Robolectric or Android stubs.

---

### 1.2 LiveActivityManager Actor Model
- **Objective:** Eliminate race conditions between concurrent signal streams and provide ordered state updates with single deadline scheduling (ADR-05).
- **Implementation:**
  - Commands enter via a bounded `Channel<EngineCommand>(capacity = 128)`.
  - State reduction runs sequentially in an actor loop, publishing updates to `StateFlow<InternalIslandState>`.
  - Single deadline scheduler: dynamically calculates the earliest upcoming event (e.g. 8s auto-collapse, 2.5s minDisplay) and schedules a single `delay()` coroutine `Job`.
- **Verification Evidence:**
  - `LiveActivityManagerTest` verifies auto-collapse from `IslandPresentation.EXPANDED` back to `COMPACT` after advancing virtual time past 8000ms.

---

### 1.3 Priority Policy & Preemption Guard
- **Objective:** Determine which activity occupies the primary cutout pill and prevent thrashing from low-priority incoming notifications.
- **Implementation:**
  - Mathematical ranking: `EffectivePriority = basePriority + urgencyBoost + interactionBoost - staleness`.
  - Preemption guard: active items visible for `< 2500ms` cannot be preempted by newcomer activities unless the newcomer is `ActivityPriority.CRITICAL` (`+60` urgency boost).
- **Verification Evidence:**
  - `PriorityPolicyTest` validates that normal incoming notifications are blocked from preempting active media within the 2500ms window, but critical alarms/timers preempt immediately.

---

### 1.4 EventProcessor Deduplication & Rate Limiting
- **Objective:** Filter bursty signals (e.g. chat app bursts, rapid track change metadata updates) before reaching the actor channel.
- **Implementation:**
  - `EventProcessor` merges collector flows and gates on permissions and settings.
  - Implements a 500ms sliding deduplication window on identical `(ActivityKey, contentHash)`.
- **Verification Evidence:**
  - `EventProcessorTest` verifies duplicate events emitted back-to-back within 500ms are cleanly dropped.

---

### 1.5 Overlay Gestures & Compose Rendering
- **Objective:** Enable user interactions and render active activities across Compact, Expanded, and Split states.
- **Implementation:**
  - `GestureHandler` maps horizontal drags to `UserIntent.Dismiss` and vertical upward drags to `UserIntent.Collapse`.
  - `IslandRenderer` composes primary, secondary, and slot contents based on `LiveActivityManager.InternalIslandState`.
- **Verification Evidence:**
  - `GestureHandlerTest` passes threshold drag validations.
  - `IslandRenderer` and `IslandPill` compile cleanly in `:overlay:assembleDebug`.

---

## 2. Verification Summary

| Component | Module | Status | Evidence |
|---|---|---|---|
| Domain Models & Commands | `:core:model` | Passed | `EngineCommandTest` passed |
| Priority Policy | `:engine` | Passed | `PriorityPolicyTest` passed (preemption & urgency) |
| Activity Registry | `:engine` | Passed | `ActivityRegistryTest` passed (LRU eviction) |
| Lifecycle Scheduler | `:engine` | Passed | `LifecycleSchedulerTest` passed (earliest deadline) |
| LiveActivityManager Actor | `:engine` | Passed | `LiveActivityManagerTest` passed (actor & 8s auto-collapse) |
| Event Processor | `:engine` | Passed | `EventProcessorTest` passed (500ms dedupe) |
| Gesture Handler | `:overlay` | Passed | `GestureHandlerTest` passed (drag thresholds) |
| Island Compose Renderer | `:overlay` | Passed | `:overlay:assembleDebug` and unit tests passed |
| Full Project Build & Tests | All 14 modules | Passed | `./gradlew test` (342 actionable tasks, 0 failures) |
