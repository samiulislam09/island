# Milestone 3: Engine & Activity Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the central `:engine` subsystem (EventProcessor, LiveActivityManager actor, PriorityPolicy, ActivityRegistry, LifecycleScheduler) and integrate it with basic `:overlay` renderers and gesture handlers.

**Architecture:** Pure JVM `:engine` module with zero Android SDK dependencies. Event flow: `EventProcessor` filters/coalesces/dedupes collector events and feeds `Channel<EngineCommand>(128)` into `LiveActivityManager` actor. The actor performs pure state reduction, updates `StateFlow<IslandState>`, and manages a single unified deadline `Job`. `IslandRenderer` in `:overlay` displays active activities, while `GestureHandler` translates user interactions into `UserIntent` commands.

**Tech Stack:** Kotlin 2.0.21, Kotlinx Coroutines 1.8.1 (`runTest`, `Channel`, `StateFlow`, `Turbine`), Jetpack Compose, JUnit 4/5, Google Truth.

**Spec:** `docs/superpowers/specs/2026-11-11-milestone-3-engine-activity-management-design.md`

## Global Constraints
- Pure JVM `:engine`: Strictly zero Android SDK imports (`android.*`) in `:engine`.
- User Constraint: "dont commit automatically" — NEVER execute automated `git commit` commands; verify each task with tests and update the SDD ledger.
- Strict TDD: Follow RED-GREEN-REFACTOR for all tasks.
- Privacy: Never log or persist notification bodies or contact names (ADR-07).

## Review Focus
1. Rapid notification bursts (>10 in 1s) must not crash or thrash the actor channel.
2. An expanded activity must never be preempted by non-urgent incoming alerts.
3. Visible activities must not be preempted before their 2.5s minimum display time unless urgency ≥ +60.
4. Auto-collapse must trigger precisely after 8.0s of inactivity in the expanded state.
5. Pausing media must linger for 30s before dismissal unless a new track begins.

---

### Task 1: Domain Models for Engine & Lifecycle (`:core:model` & `:engine`)

**Files:**
- Create: `core/model/src/main/kotlin/io/selorax/island/model/EngineCommand.kt`
- Create: `core/model/src/main/kotlin/io/selorax/island/model/EngineEffect.kt`
- Create: `core/model/src/main/kotlin/io/selorax/island/model/PriorityModels.kt`
- Test: `core/model/src/test/kotlin/io/selorax/island/model/EngineCommandTest.kt`

**Interfaces:**
- Consumes: `IslandEvent`, `UserIntent`, `SystemEvent`
- Produces: `EngineCommand`, `EngineEffect`, `EffectivePriority`, `IslandPresentation`

- [ ] **Step 1: Write failing unit test `EngineCommandTest`**

`core/model/src/test/kotlin/io/selorax/island/model/EngineCommandTest.kt`:
```kotlin
package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EngineCommandTest {
    @Test
    fun `engine commands and effects can be instantiated`() {
        val event = IslandEvent.End(
            key = ActivityKey("media:spotify"),
            source = EventSource.MEDIA,
            timestamp = 1000L
        )
        val cmd = EngineCommand.ApplyEvent(event)
        assertThat(cmd.event).isEqualTo(event)

        val effect = EngineEffect.TriggerHaptic(HapticType.LIGHT)
        assertThat(effect.effectType).isEqualTo(HapticType.LIGHT)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:model:test
```
Expected: FAIL (unresolved references `EngineCommand`, `EngineEffect`, `HapticType`).

- [ ] **Step 3: Implement domain models in `:core:model`**

`core/model/src/main/kotlin/io/selorax/island/model/EngineCommand.kt`:
```kotlin
package io.selorax.island.model

public sealed interface EngineCommand {
    public data class ApplyEvent(public val event: IslandEvent) : EngineCommand
    public data class HandleIntent(public val intent: UserIntent) : EngineCommand
    public data class Tick(public val currentEpochMs: Long) : EngineCommand
    public data class SetSystemState(public val event: SystemEvent) : EngineCommand
}
```

`core/model/src/main/kotlin/io/selorax/island/model/EngineEffect.kt`:
```kotlin
package io.selorax.island.model

public enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    WARNING
}

public sealed interface EngineEffect {
    public data class TriggerHaptic(public val effectType: HapticType) : EngineEffect
    public data class LaunchIntent(public val actionId: String, public val activityKey: ActivityKey) : EngineEffect
}
```

`core/model/src/main/kotlin/io/selorax/island/model/PriorityModels.kt`:
```kotlin
package io.selorax.island.model

public data class EffectivePriority(
    public val score: Int,
    public val isUrgent: Boolean = false
) : Comparable<EffectivePriority> {
    override fun compareTo(other: EffectivePriority): Int = score.compareTo(other.score)
}

public enum class IslandPresentation {
    MINIMAL,
    COMPACT,
    SPLIT,
    EXPANDED
}

public enum class IslandVisibility {
    VISIBLE,
    HIDDEN_LANDSCAPE,
    HIDDEN_IMMERSIVE,
    HIDDEN_KEYGUARD,
    HIDDEN_SNOOZED,
    HIDDEN_DISABLED
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :core:model:test
```
Expected: PASS.

---

### Task 2: Priority Policy (`:engine`)

**Files:**
- Create: `engine/src/main/kotlin/io/selorax/island/engine/PriorityPolicy.kt`
- Test: `engine/src/test/kotlin/io/selorax/island/engine/PriorityPolicyTest.kt`

**Interfaces:**
- Consumes: `LiveActivity`, `EventSource`, `EffectivePriority`
- Produces: `PriorityPolicy.computePriority()`, `PriorityPolicy.canPreempt()`, `PriorityPolicy.rank()`

- [ ] **Step 1: Write failing unit test `PriorityPolicyTest`**

`engine/src/test/kotlin/io/selorax/island/engine/PriorityPolicyTest.kt`:
```kotlin
package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EffectivePriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity
import org.junit.Test

class PriorityPolicyTest {
    private val policy = PriorityPolicy()

    private fun createActivity(
        key: String,
        source: EventSource,
        urgency: ActivityPriority = ActivityPriority.DEFAULT,
        visibleSince: Long = 0L,
        updatedAt: Long = 1000L
    ): LiveActivity {
        return LiveActivity(
            key = ActivityKey(key),
            source = source,
            priority = urgency,
            state = IslandState.Compact(IslandSlot.Icon("test"), IslandSlot.Text("label")),
            createdAt = 1000L,
            updatedAt = updatedAt,
            visibleSince = visibleSince
        )
    }

    @Test
    fun `urgent timer has higher priority than normal media`() {
        val timer = createActivity("timer:1", EventSource.TIMER, urgency = ActivityPriority.URGENT)
        val media = createActivity("media:spotify", EventSource.MEDIA, urgency = ActivityPriority.DEFAULT)

        val timerPriority = policy.computePriority(timer, currentEpochMs = 1000L, isExpanded = false)
        val mediaPriority = policy.computePriority(media, currentEpochMs = 1000L, isExpanded = false)

        assertThat(timerPriority.score).isGreaterThan(mediaPriority.score)
    }

    @Test
    fun `preemption guard blocks non-urgent newcomer before minDisplay elapsed`() {
        val active = createActivity("media:spotify", EventSource.MEDIA, visibleSince = 1000L)
        val newcomer = createActivity("notif:app:1", EventSource.NOTIFICATION, urgency = ActivityPriority.DEFAULT)

        // Only 1000ms elapsed since active became visible (minDisplay is 2500ms)
        val canPreempt = policy.canPreempt(active, newcomer, currentEpochMs = 2000L)
        assertThat(canPreempt).isFalse()
    }

    @Test
    fun `preemption guard allows urgent newcomer before minDisplay elapsed`() {
        val active = createActivity("media:spotify", EventSource.MEDIA, visibleSince = 1000L)
        val urgentTimer = createActivity("timer:1", EventSource.TIMER, urgency = ActivityPriority.URGENT)

        val canPreempt = policy.canPreempt(active, urgentTimer, currentEpochMs = 2000L)
        assertThat(canPreempt).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :engine:test
```
Expected: FAIL (unresolved class `PriorityPolicy` and missing `visibleSince` on `LiveActivity`).

- [ ] **Step 3: Update `LiveActivity` and implement `PriorityPolicy`**

Modify `core/model/src/main/kotlin/io/selorax/island/model/LiveActivity.kt` to include `visibleSince: Long? = null`:
```kotlin
package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public data class ActivityKey(public val raw: String)

public enum class ActivityPriority {
    LOW,
    DEFAULT,
    HIGH,
    URGENT
}

@Serializable
public data class LiveActivity(
    public val key: ActivityKey,
    public val source: EventSource,
    public val priority: ActivityPriority = ActivityPriority.DEFAULT,
    public val state: IslandState,
    public val createdAt: Long,
    public val updatedAt: Long,
    public val visibleSince: Long? = null
)
```

Create `engine/src/main/kotlin/io/selorax/island/engine/PriorityPolicy.kt`:
```kotlin
package io.selorax.island.engine

import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EffectivePriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.LiveActivity

public class PriorityPolicy(
    public val minDisplayMs: Long = 2500L
) {
    public fun computePriority(
        activity: LiveActivity,
        currentEpochMs: Long,
        isExpanded: Boolean
    ): EffectivePriority {
        val base = when (activity.source) {
            EventSource.TIMER -> 90
            EventSource.BATTERY -> 85
            EventSource.MEDIA -> 70
            EventSource.NOTIFICATION -> 50
            EventSource.SYSTEM -> 40
        }

        val urgencyBoost = when (activity.priority) {
            ActivityPriority.URGENT -> 60
            ActivityPriority.HIGH -> 30
            ActivityPriority.DEFAULT -> 0
            ActivityPriority.LOW -> -20
        }

        val interactionBoost = if (isExpanded) 30 else 0

        // Staleness penalty: -1 per 10s up to -20
        val ageSeconds = (currentEpochMs - activity.updatedAt).coerceAtLeast(0L) / 1000L
        val staleness = (ageSeconds / 10L).toInt().coerceAtMost(20)

        val finalScore = base + urgencyBoost + interactionBoost - staleness
        return EffectivePriority(score = finalScore, isUrgent = activity.priority == ActivityPriority.URGENT)
    }

    public fun canPreempt(
        active: LiveActivity,
        newcomer: LiveActivity,
        currentEpochMs: Long
    ): Boolean {
        if (newcomer.priority == ActivityPriority.URGENT) return true
        val visibleSince = active.visibleSince ?: return true
        val displayTime = currentEpochMs - visibleSince
        return displayTime >= minDisplayMs
    }

    public fun rank(
        activities: List<LiveActivity>,
        currentEpochMs: Long,
        expandedKey: String?
    ): List<LiveActivity> {
        return activities.sortedByDescending { activity ->
            computePriority(activity, currentEpochMs, activity.key.raw == expandedKey).score
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :engine:test
```
Expected: PASS.

---

### Task 3: Activity Registry (`:engine`)

**Files:**
- Create: `engine/src/main/kotlin/io/selorax/island/engine/ActivityRegistry.kt`
- Test: `engine/src/test/kotlin/io/selorax/island/engine/ActivityRegistryTest.kt`

**Interfaces:**
- Consumes: `LiveActivity`, `ActivityKey`
- Produces: `ActivityRegistry` managing up to 20 activities with upsert, remove, expiry, and list operations

- [ ] **Step 1: Write failing unit test `ActivityRegistryTest`**

`engine/src/test/kotlin/io/selorax/island/engine/ActivityRegistryTest.kt`:
```kotlin
package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity
import org.junit.Test

class ActivityRegistryTest {
    private val registry = ActivityRegistry(maxItems = 3)

    private fun create(key: String, updatedAt: Long): LiveActivity {
        return LiveActivity(
            key = ActivityKey(key),
            source = EventSource.NOTIFICATION,
            state = IslandState.Compact(IslandSlot.Icon("icon"), IslandSlot.Text(key)),
            createdAt = updatedAt,
            updatedAt = updatedAt
        )
    }

    @Test
    fun `upsert updates existing item`() {
        registry.upsert(create("1", 100L))
        registry.upsert(create("1", 200L))

        assertThat(registry.size).isEqualTo(1)
        assertThat(registry.get(ActivityKey("1"))?.updatedAt).isEqualTo(200L)
    }

    @Test
    fun `capacity eviction removes oldest item`() {
        registry.upsert(create("1", 100L))
        registry.upsert(create("2", 200L))
        registry.upsert(create("3", 300L))
        registry.upsert(create("4", 400L))

        assertThat(registry.size).isEqualTo(3)
        assertThat(registry.get(ActivityKey("1"))).isNull()
        assertThat(registry.get(ActivityKey("4"))).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :engine:test
```
Expected: FAIL (unresolved class `ActivityRegistry`).

- [ ] **Step 3: Implement `ActivityRegistry`**

`engine/src/main/kotlin/io/selorax/island/engine/ActivityRegistry.kt`:
```kotlin
package io.selorax.island.engine

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.LiveActivity

public class ActivityRegistry(
    private val maxItems: Int = 20
) {
    private val items = mutableMapOf<ActivityKey, LiveActivity>()

    public val size: Int get() = items.size

    public fun get(key: ActivityKey): LiveActivity? = items[key]

    public fun getAll(): List<LiveActivity> = items.values.toList()

    public fun upsert(activity: LiveActivity) {
        if (!items.containsKey(activity.key) && items.size >= maxItems) {
            // Evict oldest by updatedAt
            val oldestKey = items.minByOrNull { it.value.updatedAt }?.key
            if (oldestKey != null) {
                items.remove(oldestKey)
            }
        }
        items[activity.key] = activity
    }

    public fun remove(key: ActivityKey): LiveActivity? = items.remove(key)

    public fun clear() {
        items.clear()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :engine:test
```
Expected: PASS.

---

### Task 4: Lifecycle Scheduler & Deadline Calculator (`:engine`)

**Files:**
- Create: `engine/src/main/kotlin/io/selorax/island/engine/LifecycleScheduler.kt`
- Test: `engine/src/test/kotlin/io/selorax/island/engine/LifecycleSchedulerTest.kt`

**Interfaces:**
- Consumes: Active activities, expanded state timestamp, auto-collapse duration
- Produces: Nearest deadline epoch ms or null

- [ ] **Step 1: Write failing unit test `LifecycleSchedulerTest`**

`engine/src/test/kotlin/io/selorax/island/engine/LifecycleSchedulerTest.kt`:
```kotlin
package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LifecycleSchedulerTest {
    private val scheduler = LifecycleScheduler()

    @Test
    fun `calculates earliest deadline between auto-collapse and minDisplay`() {
        val deadlines = listOf(5000L, 8000L, 3000L)
        val nearest = scheduler.earliestDeadline(deadlines, currentEpochMs = 1000L)

        assertThat(nearest).isEqualTo(3000L)
    }

    @Test
    fun `ignores past deadlines`() {
        val deadlines = listOf(500L, 800L, 3000L)
        val nearest = scheduler.earliestDeadline(deadlines, currentEpochMs = 1000L)

        assertThat(nearest).isEqualTo(3000L)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :engine:test
```
Expected: FAIL (unresolved class `LifecycleScheduler`).

- [ ] **Step 3: Implement `LifecycleScheduler`**

`engine/src/main/kotlin/io/selorax/island/engine/LifecycleScheduler.kt`:
```kotlin
package io.selorax.island.engine

public class LifecycleScheduler {
    public fun earliestDeadline(
        deadlines: List<Long>,
        currentEpochMs: Long
    ): Long? {
        return deadlines
            .filter { it > currentEpochMs }
            .minOrNull()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :engine:test
```
Expected: PASS.

---

### Task 5: LiveActivityManager Actor (`:engine`)

**Files:**
- Create: `engine/src/main/kotlin/io/selorax/island/engine/LiveActivityManager.kt`
- Test: `engine/src/test/kotlin/io/selorax/island/engine/LiveActivityManagerTest.kt`

**Interfaces:**
- Consumes: `EngineCommand`, CoroutineScope
- Produces: `StateFlow<IslandState>`, `Flow<EngineEffect>`

- [ ] **Step 1: Write failing unit test `LiveActivityManagerTest`**

`engine/src/test/kotlin/io/selorax/island/engine/LiveActivityManagerTest.kt`:
```kotlin
package io.selorax.island.engine

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EngineCommand
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import io.selorax.island.model.UserIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveActivityManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val manager = LiveActivityManager(
        scope = testScope,
        priorityPolicy = PriorityPolicy(),
        registry = ActivityRegistry(),
        scheduler = LifecycleScheduler()
    )

    @Test
    fun `applying upsert event shows primary activity`() = testScope.runTest {
        manager.state.test {
            val initial = awaitItem()
            assertThat(initial.primary).isNull()

            val event = IslandEvent.Upsert(
                key = ActivityKey("media:spotify"),
                source = EventSource.MEDIA,
                timestamp = testScheduler.currentTime,
                state = IslandState.Compact(IslandSlot.Icon("music"), IslandSlot.Text("Song")),
                hints = PriorityHints()
            )
            manager.send(EngineCommand.ApplyEvent(event))

            val updated = awaitItem()
            assertThat(updated.primary?.key?.raw).isEqualTo("media:spotify")
            assertThat(updated.presentation).isEqualTo(IslandPresentation.COMPACT)
        }
    }

    @Test
    fun `tapping compact activity expands it and collapses after 8s`() = testScope.runTest {
        manager.state.test {
            awaitItem() // initial

            val event = IslandEvent.Upsert(
                key = ActivityKey("timer:1"),
                source = EventSource.TIMER,
                timestamp = testScheduler.currentTime,
                state = IslandState.Compact(IslandSlot.Icon("timer"), IslandSlot.Text("02:00")),
                hints = PriorityHints()
            )
            manager.send(EngineCommand.ApplyEvent(event))
            awaitItem() // compact

            // Tap to expand
            manager.send(EngineCommand.HandleIntent(UserIntent.Tap))
            val expanded = awaitItem()
            assertThat(expanded.presentation).isEqualTo(IslandPresentation.EXPANDED)

            // Advance time past 8s auto-collapse
            testScheduler.advanceTimeBy(8100L)
            val collapsed = awaitItem()
            assertThat(collapsed.presentation).isEqualTo(IslandPresentation.COMPACT)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :engine:test
```
Expected: FAIL (unresolved class `LiveActivityManager`).

- [ ] **Step 3: Implement `LiveActivityManager`**

`engine/src/main/kotlin/io/selorax/island/engine/LiveActivityManager.kt`:
```kotlin
package io.selorax.island.engine

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EngineCommand
import io.selorax.island.model.EngineEffect
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.IslandState
import io.selorax.island.model.IslandVisibility
import io.selorax.island.model.LiveActivity
import io.selorax.island.model.UserIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

public class LiveActivityManager(
    private val scope: CoroutineScope,
    private val priorityPolicy: PriorityPolicy,
    private val registry: ActivityRegistry,
    private val scheduler: LifecycleScheduler,
    private val autoCollapseMs: Long = 8000L
) {
    public data class InternalIslandState(
        public val visibility: IslandVisibility = IslandVisibility.VISIBLE,
        public val presentation: IslandPresentation = IslandPresentation.COMPACT,
        public val primary: LiveActivity? = null,
        public val secondary: LiveActivity? = null,
        public val queuedCount: Int = 0,
        public val expandedSince: Long? = null
    )

    private val _state = MutableStateFlow(InternalIslandState())
    public val state: StateFlow<InternalIslandState> = _state.asStateFlow()

    private val _effects = Channel<EngineEffect>(Channel.BUFFERED)
    public val effects: Flow<EngineEffect> = _effects.receiveAsFlow()

    private val commandChannel = Channel<EngineCommand>(capacity = 128)
    private var deadlineJob: Job? = null

    init {
        scope.launch {
            for (command in commandChannel) {
                reduce(command)
                scheduleNextDeadline()
            }
        }
    }

    public fun send(command: EngineCommand) {
        commandChannel.trySend(command)
    }

    private fun reduce(command: EngineCommand) {
        val currentEpoch = System.currentTimeMillis()
        val current = _state.value

        when (command) {
            is EngineCommand.ApplyEvent -> handleEvent(command.event, currentEpoch)
            is EngineCommand.HandleIntent -> handleIntent(command.intent, currentEpoch)
            is EngineCommand.Tick -> handleTick(command.currentEpochMs)
            is EngineCommand.SetSystemState -> { /* handled in Task 6 */ }
        }
    }

    private fun handleEvent(event: IslandEvent, now: Long) {
        when (event) {
            is IslandEvent.Upsert -> {
                val existing = registry.get(event.key)
                val visibleSince = existing?.visibleSince ?: now
                val activity = LiveActivity(
                    key = event.key,
                    source = event.source,
                    priority = ActivityPriority.DEFAULT,
                    state = event.state,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    visibleSince = visibleSince
                )
                registry.upsert(activity)
                recomputeState(now)
            }
            is IslandEvent.End -> {
                registry.remove(event.key)
                recomputeState(now)
            }
            is IslandEvent.Alert -> {
                val activity = LiveActivity(
                    key = event.key,
                    source = event.source,
                    priority = ActivityPriority.HIGH,
                    state = event.state,
                    createdAt = now,
                    updatedAt = now,
                    visibleSince = now
                )
                registry.upsert(activity)
                recomputeState(now)
            }
            is IslandEvent.Batch -> {
                event.events.forEach { handleEvent(it, now) }
            }
        }
    }

    private fun handleIntent(intent: UserIntent, now: Long) {
        val current = _state.value
        when (intent) {
            is UserIntent.Tap -> {
                if (current.presentation == IslandPresentation.COMPACT) {
                    _state.value = current.copy(
                        presentation = IslandPresentation.EXPANDED,
                        expandedSince = now
                    )
                } else if (current.presentation == IslandPresentation.EXPANDED) {
                    _state.value = current.copy(
                        presentation = IslandPresentation.COMPACT,
                        expandedSince = null
                    )
                }
            }
            is UserIntent.Collapse -> {
                _state.value = current.copy(
                    presentation = IslandPresentation.COMPACT,
                    expandedSince = null
                )
            }
            is UserIntent.Dismiss -> {
                val primaryKey = current.primary?.key
                if (primaryKey != null) {
                    registry.remove(primaryKey)
                    recomputeState(now)
                }
            }
            else -> { /* Other gestures */ }
        }
    }

    private fun handleTick(now: Long) {
        val current = _state.value
        // Auto-collapse check
        val expandedSince = current.expandedSince
        if (current.presentation == IslandPresentation.EXPANDED && expandedSince != null) {
            if (now - expandedSince >= autoCollapseMs) {
                _state.value = current.copy(
                    presentation = IslandPresentation.COMPACT,
                    expandedSince = null
                )
            }
        }
        recomputeState(now)
    }

    private fun recomputeState(now: Long) {
        val ranked = priorityPolicy.rank(registry.getAll(), now, _state.value.primary?.key?.raw)
        val primary = ranked.firstOrNull()
        val secondary = ranked.getOrNull(1)
        val queued = (ranked.size - 2).coerceAtLeast(0)

        _state.value = _state.value.copy(
            primary = primary,
            secondary = secondary,
            queuedCount = queued
        )
    }

    private fun scheduleNextDeadline() {
        deadlineJob?.cancel()
        val current = _state.value
        val now = System.currentTimeMillis()

        val deadlines = mutableListOf<Long>()
        val expandedSince = current.expandedSince
        if (current.presentation == IslandPresentation.EXPANDED && expandedSince != null) {
            deadlines.add(expandedSince + autoCollapseMs)
        }

        val nearest = scheduler.earliestDeadline(deadlines, now) ?: return
        val delayMs = (nearest - now).coerceAtLeast(10L)

        deadlineJob = scope.launch {
            delay(delayMs)
            send(EngineCommand.Tick(System.currentTimeMillis()))
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :engine:test
```
Expected: PASS.

---

### Task 6: EventProcessor Pipeline (`:engine`)

**Files:**
- Create: `engine/src/main/kotlin/io/selorax/island/engine/EventProcessor.kt`
- Test: `engine/src/test/kotlin/io/selorax/island/engine/EventProcessorTest.kt`

**Interfaces:**
- Consumes: `Set<EventCollector>`, `Flow<IslandSettings>`, `Flow<PermissionSnapshot>`
- Produces: `EventProcessor.process(): Flow<IslandEvent>` with dedupe and rate limiting

- [ ] **Step 1: Write failing unit test `EventProcessorTest`**

`engine/src/test/kotlin/io/selorax/island/engine/EventProcessorTest.kt`:
```kotlin
package io.selorax.island.engine

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class EventProcessorTest {
    private class FakeCollector(override val source: EventSource) : EventCollector {
        val flow = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 16)
        override val events: Flow<IslandEvent> = flow.asSharedFlow()
    }

    private val mediaCollector = FakeCollector(EventSource.MEDIA)
    private val processor = EventProcessor(
        collectors = setOf(mediaCollector),
        isNotificationAllowed = MutableStateFlow(true)
    )

    @Test
    fun `deduplicates identical events within 500ms window`() = runBlocking {
        processor.process().test {
            val event = IslandEvent.Upsert(
                key = ActivityKey("media:spotify"),
                source = EventSource.MEDIA,
                timestamp = 1000L,
                state = IslandState.Compact(IslandSlot.Icon("music"), IslandSlot.Text("Song")),
                hints = PriorityHints()
            )

            mediaCollector.flow.tryEmit(event)
            mediaCollector.flow.tryEmit(event) // Duplicate

            val received = awaitItem()
            assertThat(received.key.raw).isEqualTo("media:spotify")
            expectNoEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :engine:test
```
Expected: FAIL (unresolved class `EventProcessor`).

- [ ] **Step 3: Implement `EventProcessor`**

`engine/src/main/kotlin/io/selorax/island/engine/EventProcessor.kt`:
```kotlin
package io.selorax.island.engine

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge

public class EventProcessor(
    private val collectors: Set<EventCollector>,
    private val isNotificationAllowed: StateFlow<Boolean>
) {
    private val lastSeen = mutableMapOf<ActivityKey, Long>()
    private val dedupeWindowMs = 500L

    public fun process(): Flow<IslandEvent> {
        val allFlows = collectors.map { it.events }
        return allFlows.merge()
            .filter { event ->
                if (event.source == EventSource.NOTIFICATION && !isNotificationAllowed.value) {
                    return@filter false
                }
                val now = System.currentTimeMillis()
                val last = lastSeen[event.key]
                if (last != null && (now - last) < dedupeWindowMs) {
                    false
                } else {
                    lastSeen[event.key] = now
                    true
                }
            }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :engine:test
```
Expected: PASS.

---

### Task 7: Gesture Handler (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/GestureHandler.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/GestureHandlerTest.kt`

**Interfaces:**
- Consumes: Compose PointerInputScope gestures
- Produces: `UserIntent` (`Tap`, `LongPress`, `SwipeHorizontal`, `Collapse`, `Dismiss`)

- [ ] **Step 1: Write failing unit test `GestureHandlerTest`**

`overlay/src/test/kotlin/io/selorax/island/overlay/GestureHandlerTest.kt`:
```kotlin
package io.selorax.island.overlay

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.UserIntent
import org.junit.Test

class GestureHandlerTest {
    private val handler = GestureHandler()

    @Test
    fun `horizontal drag exceeding threshold returns dismiss intent`() {
        val intent = handler.onDragEnd(dragAmountX = 150f, dragAmountY = 0f, thresholdPx = 100f)
        assertThat(intent).isEqualTo(UserIntent.Dismiss)
    }

    @Test
    fun `vertical upward drag exceeding threshold returns collapse intent`() {
        val intent = handler.onDragEnd(dragAmountX = 0f, dragAmountY = -120f, thresholdPx = 100f)
        assertThat(intent).isEqualTo(UserIntent.Collapse)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :overlay:testDebugUnitTest
```
Expected: FAIL (unresolved class `GestureHandler`).

- [ ] **Step 3: Implement `GestureHandler`**

`overlay/src/main/kotlin/io/selorax/island/overlay/GestureHandler.kt`:
```kotlin
package io.selorax.island.overlay

import io.selorax.island.model.UserIntent
import kotlin.math.abs

public class GestureHandler {
    public fun onDragEnd(
        dragAmountX: Float,
        dragAmountY: Float,
        thresholdPx: Float = 100f
    ): UserIntent? {
        return when {
            dragAmountY < -thresholdPx -> UserIntent.Collapse
            abs(dragAmountX) > thresholdPx -> UserIntent.Dismiss
            else -> null
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :overlay:testDebugUnitTest
```
Expected: PASS.

---

### Task 8: Activity Renderers & Overlay Integration (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandRenderer.kt`
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandPill.kt`

**Interfaces:**
- Consumes: `LiveActivityManager.InternalIslandState`, `UserIntent` callback
- Produces: Compose UI rendering Compact, Expanded, and Split activities

- [ ] **Step 1: Implement `IslandRenderer`**

`overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandRenderer.kt`:
```kotlin
package io.selorax.island.overlay.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.engine.LiveActivityManager
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.UserIntent

@Composable
public fun IslandRenderer(
    state: LiveActivityManager.InternalIslandState,
    onIntent: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = state.primary ?: return

    Box(
        modifier = modifier.clickable { onIntent(UserIntent.Tap) },
        contentAlignment = Alignment.Center
    ) {
        when (state.presentation) {
            IslandPresentation.COMPACT -> RenderCompact(primary.state)
            IslandPresentation.EXPANDED -> RenderExpanded(primary.state)
            IslandPresentation.SPLIT -> RenderSplit(primary.state, state.secondary?.state)
            IslandPresentation.MINIMAL -> RenderCompact(primary.state)
        }
    }
}

@Composable
private fun RenderCompact(state: IslandState) {
    if (state is IslandState.Compact) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderSlot(state.leadingSlot)
            Spacer(modifier = Modifier.width(8.dp))
            RenderSlot(state.trailingSlot)
        }
    }
}

@Composable
private fun RenderExpanded(state: IslandState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (state is IslandState.Compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RenderSlot(state.leadingSlot)
                Spacer(modifier = Modifier.width(12.dp))
                RenderSlot(state.trailingSlot)
            }
        }
    }
}

@Composable
private fun RenderSplit(primary: IslandState, secondary: IslandState?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RenderCompact(primary)
        if (secondary != null) {
            Spacer(modifier = Modifier.width(8.dp))
            RenderCompact(secondary)
        }
    }
}

@Composable
private fun RenderSlot(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(text = slot.text, color = Color.White, fontSize = 12.sp)
        is IslandSlot.Icon -> Text(text = "●", color = Color.White, fontSize = 12.sp)
        is IslandSlot.Progress -> Text(text = "${(slot.progress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}
```

- [ ] **Step 2: Update `IslandPill` to integrate `IslandRenderer`**

Ensure `IslandPill` can take `LiveActivityManager.InternalIslandState` and `onIntent: (UserIntent) -> Unit`.

- [ ] **Step 3: Verify overlay compilation**

Run:
```bash
./gradlew :overlay:assembleDebug :overlay:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all unit tests pass.

---

### Task 9: Full End-to-End Verification & Week 3 Findings Report

**Files:**
- Create: `docs/prototypes/week3-findings.md`

**Interfaces:**
- Consumes: All 14 modules, full test suite
- Produces: Green test suite across the entire project and documented Week 3 prototype report

- [ ] **Step 1: Run complete build and test suite**

Run:
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL, 0 failures across all modules.

- [ ] **Step 2: Document Milestone 3 findings in `docs/prototypes/week3-findings.md`**

Record findings for:
- Actor loop latency and virtual-time execution.
- PriorityPolicy preemption guard behavior.
- EventProcessor deduplication and rate-limiting.
- Gesture handling and Compose rendering integration.
