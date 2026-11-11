package io.selorax.island.feature.battery

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BatteryCollectorTest {
    private val collector = BatteryCollector()

    @Test
    fun `plugging charger in emits charging alert`() = runBlocking {
        collector.events.test {
            collector.onBatteryStateChanged(level = 80, isCharging = true)

            val event = awaitItem() as IslandEvent.Alert
            assertThat(event.key.raw).isEqualTo("battery:alert")
            assertThat(event.durationMs).isEqualTo(3000L)
        }
    }

    @Test
    fun `low battery emits low battery alert`() = runBlocking {
        collector.events.test {
            collector.onBatteryStateChanged(level = 14, isCharging = false)

            val event = awaitItem() as IslandEvent.Alert
            assertThat(event.key.raw).isEqualTo("battery:alert")
            assertThat(event.durationMs).isEqualTo(4000L)
        }
    }
}
