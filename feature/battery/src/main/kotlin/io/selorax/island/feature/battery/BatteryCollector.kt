package io.selorax.island.feature.battery

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class BatteryCollector : EventCollector {
    override val source: EventSource = EventSource.BATTERY

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 16)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    private var lastPluggedState: Boolean? = null
    private var lastLevelAlerted: Int? = null

    public fun onBatteryStateChanged(level: Int, isCharging: Boolean) {
        // Charging plug-in alert
        if (isCharging && lastPluggedState != true) {
            emitAlert("Charging $level%", durationMs = 3000L)
        } else if (!isCharging && lastPluggedState == true) {
            emitAlert("Unplugged", durationMs = 2000L)
        }

        // Low battery alert threshold (15% or 20%)
        if (!isCharging && level <= 15 && lastLevelAlerted != level) {
            emitAlert("Low Battery $level%", durationMs = 4000L)
            lastLevelAlerted = level
        }

        lastPluggedState = isCharging
    }

    private fun emitAlert(message: String, durationMs: Long) {
        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text(message),
            centerContent = null
        )
        val event = IslandEvent.Alert(
            key = ActivityKey("battery:alert"),
            source = EventSource.BATTERY,
            timestamp = System.currentTimeMillis(),
            state = state,
            durationMs = durationMs
        )
        _events.tryEmit(event)
    }
}
