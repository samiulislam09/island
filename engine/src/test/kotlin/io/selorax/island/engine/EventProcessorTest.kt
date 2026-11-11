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
import org.junit.jupiter.api.Test

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
