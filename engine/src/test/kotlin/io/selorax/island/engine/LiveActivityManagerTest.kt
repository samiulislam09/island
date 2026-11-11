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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveActivityManagerTest {

    private fun createManager(scope: CoroutineScope, clock: () -> Long): LiveActivityManager {
        return LiveActivityManager(
            scope = scope,
            priorityPolicy = PriorityPolicy(),
            registry = ActivityRegistry(),
            scheduler = LifecycleScheduler(),
            clock = clock
        )
    }

    @Test
    fun `applying upsert event shows primary activity`() = runTest {
        val manager = createManager(backgroundScope, clock = { testScheduler.currentTime })
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
    fun `tapping compact activity expands it and collapses after 8s`() = runTest {
        val manager = createManager(backgroundScope, clock = { testScheduler.currentTime })
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
            manager.send(EngineCommand.HandleIntent(UserIntent.Tap()))
            val expanded = awaitItem()
            assertThat(expanded.presentation).isEqualTo(IslandPresentation.EXPANDED)

            // Advance time past 8s auto-collapse
            testScheduler.advanceTimeBy(8100L)
            val collapsed = awaitItem()
            assertThat(collapsed.presentation).isEqualTo(IslandPresentation.COMPACT)
        }
    }
}
