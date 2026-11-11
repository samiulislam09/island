package io.selorax.island.feature.timer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TimerCollectorTest {
    private val collector = TimerCollector()

    @Test
    fun `timer tick emits upsert event`() = runBlocking {
        collector.events.test {
            collector.onTimerTick("1", "Eggs", remainingSeconds = 125)

            val event = awaitItem() as IslandEvent.Upsert
            assertThat(event.key.raw).isEqualTo("timer:1")
            assertThat(event.state.isVisible).isTrue()
        }
    }

    @Test
    fun `timer finished emits end event`() = runBlocking {
        collector.events.test {
            collector.onTimerFinished("1")

            val event = awaitItem() as IslandEvent.End
            assertThat(event.key.raw).isEqualTo("timer:1")
            assertThat(event.reason).isEqualTo("timer_completed")
        }
    }
}
