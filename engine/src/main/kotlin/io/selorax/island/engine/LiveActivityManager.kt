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
    private val autoCollapseMs: Long = 8000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
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
    private val actorJob: Job = scope.launch {
        for (command in commandChannel) {
            reduce(command)
            scheduleNextDeadline()
        }
    }

    public fun stop() {
        deadlineJob?.cancel()
        commandChannel.close()
        actorJob.cancel()
    }

    public fun send(command: EngineCommand) {
        commandChannel.trySend(command)
    }

    private fun reduce(command: EngineCommand) {
        val currentEpoch = clock()

        when (command) {
            is EngineCommand.ApplyEvent -> handleEvent(command.event, currentEpoch)
            is EngineCommand.HandleIntent -> handleIntent(command.intent, currentEpoch)
            is EngineCommand.Tick -> handleTick(command.currentEpochMs)
            is EngineCommand.SetSystemState -> { /* handled in future task */ }
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
        val now = clock()

        val deadlines = mutableListOf<Long>()
        val expandedSince = current.expandedSince
        if (current.presentation == IslandPresentation.EXPANDED && expandedSince != null) {
            deadlines.add(expandedSince + autoCollapseMs)
        }

        val nearest = scheduler.earliestDeadline(deadlines, now) ?: return
        val delayMs = (nearest - now).coerceAtLeast(10L)

        deadlineJob = scope.launch {
            delay(delayMs)
            send(EngineCommand.Tick(clock()))
        }
    }
}
