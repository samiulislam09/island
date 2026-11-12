package io.selorax.island.overlay

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandAction
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity
import io.selorax.island.overlay.renderers.BatteryRenderer
import io.selorax.island.overlay.renderers.MusicRenderer
import io.selorax.island.overlay.renderers.NotificationRenderer
import io.selorax.island.overlay.renderers.TimerRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RendererDataUnpackingTest {

    @Test
    fun musicRenderer_hasCorrectEventSource() {
        val renderer = MusicRenderer()
        assertEquals(EventSource.MEDIA, renderer.source)
    }

    @Test
    fun batteryRenderer_hasCorrectEventSource() {
        val renderer = BatteryRenderer()
        assertEquals(EventSource.BATTERY, renderer.source)
    }

    @Test
    fun notificationRenderer_hasCorrectEventSource() {
        val renderer = NotificationRenderer()
        assertEquals(EventSource.NOTIFICATION, renderer.source)
    }

    @Test
    fun timerRenderer_hasCorrectEventSource() {
        val renderer = TimerRenderer()
        assertEquals(EventSource.TIMER, renderer.source)
    }

    @Test
    fun liveActivity_compactAndExpandedStates_retainData() {
        val expandedState = IslandState.Expanded(
            title = "Bohemian Rhapsody",
            subtitle = "Queen",
            leadingSlot = IslandSlot.Icon("music"),
            contentSlot = IslandSlot.Progress(0.55f),
            actions = listOf(
                IslandAction("prev", "Previous"),
                IslandAction("pause", "Pause"),
                IslandAction("next", "Next")
            )
        )
        val activity = LiveActivity(
            key = ActivityKey("media_1"),
            source = EventSource.MEDIA,
            priority = ActivityPriority.DEFAULT,
            createdAt = 1000L,
            updatedAt = 1000L,
            state = expandedState
        )

        assertEquals("Bohemian Rhapsody", (activity.state as IslandState.Expanded).title)
        assertEquals("Queen", (activity.state as IslandState.Expanded).subtitle)
        assertEquals(0.55f, ((activity.state as IslandState.Expanded).contentSlot as IslandSlot.Progress).progress)
    }

    @Test
    fun notificationActivity_compactAndExpanded_retainPrivacyFields() {
        val notificationExpanded = IslandState.Expanded(
            title = "John Doe",
            subtitle = "Hey, are you free tomorrow?",
            leadingSlot = IslandSlot.Icon("mail"),
            contentSlot = null,
            actions = listOf(
                IslandAction("dismiss", "Dismiss"),
                IslandAction("open", "Open")
            )
        )
        val activity = LiveActivity(
            key = ActivityKey("notif_1"),
            source = EventSource.NOTIFICATION,
            priority = ActivityPriority.DEFAULT,
            createdAt = 2000L,
            updatedAt = 2000L,
            state = notificationExpanded
        )

        val state = activity.state as IslandState.Expanded
        assertEquals("John Doe", state.title)
        assertEquals("Hey, are you free tomorrow?", state.subtitle)
    }
}
