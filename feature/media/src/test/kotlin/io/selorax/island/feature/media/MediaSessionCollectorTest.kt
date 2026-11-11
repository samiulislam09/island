package io.selorax.island.feature.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MediaSessionCollectorTest {
    private val collector = MediaSessionCollector()

    @Test
    fun `playback start emits upsert event`() = runBlocking {
        collector.events.test {
            collector.onPlaybackStarted("com.spotify", "Blinding Lights", "The Weeknd")

            val event = awaitItem() as IslandEvent.Upsert
            assertThat(event.key.raw).isEqualTo("media:com.spotify")
            assertThat(event.state.isVisible).isTrue()
        }
    }

    @Test
    fun `playback stop emits end event`() = runBlocking {
        collector.events.test {
            collector.onPlaybackStopped("com.spotify")

            val event = awaitItem() as IslandEvent.End
            assertThat(event.key.raw).isEqualTo("media:com.spotify")
            assertThat(event.reason).isEqualTo("playback_paused")
        }
    }
}
