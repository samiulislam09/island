package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

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
