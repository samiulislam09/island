package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IslandEventTest {
    @Test
    fun `upsert event carries key source and state`() {
        val event = IslandEvent.Upsert(
            key = ActivityKey("notif:test:1"),
            source = EventSource.NOTIFICATION,
            timestamp = 1000L,
            state = IslandState.Compact(
                leadingSlot = IslandSlot.Icon("msg"),
                trailingSlot = IslandSlot.Text("Hello")
            ),
            hints = PriorityHints(priority = ActivityPriority.HIGH)
        )

        assertThat(event.key.raw).isEqualTo("notif:test:1")
        assertThat(event.source).isEqualTo(EventSource.NOTIFICATION)
        assertThat(event.hints.priority).isEqualTo(ActivityPriority.HIGH)
    }

    @Test
    fun `end event carries key and optional reason`() {
        val event = IslandEvent.End(
            key = ActivityKey("media:spotify"),
            source = EventSource.MEDIA,
            timestamp = 2000L,
            reason = "playback_stopped"
        )

        assertThat(event.key.raw).isEqualTo("media:spotify")
        assertThat(event.reason).isEqualTo("playback_stopped")
    }
}
