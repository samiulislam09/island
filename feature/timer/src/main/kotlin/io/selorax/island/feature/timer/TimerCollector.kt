package io.selorax.island.feature.timer

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class TimerCollector : EventCollector {
    override val source: EventSource = EventSource.TIMER

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 16)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun onTimerTick(id: String, label: String, remainingSeconds: Int) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val formatted = "%02d:%02d".format(minutes, seconds)

        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("timer"),
            trailingSlot = IslandSlot.Text(formatted),
            centerContent = null
        )

        val event = IslandEvent.Upsert(
            key = ActivityKey("timer:$id"),
            source = EventSource.TIMER,
            timestamp = System.currentTimeMillis(),
            state = state,
            hints = PriorityHints()
        )
        _events.tryEmit(event)
    }

    public fun onTimerFinished(id: String) {
        val event = IslandEvent.End(
            key = ActivityKey("timer:$id"),
            source = EventSource.TIMER,
            timestamp = System.currentTimeMillis(),
            reason = "timer_completed"
        )
        _events.tryEmit(event)
    }
}
