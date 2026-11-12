package io.selorax.island.overlay

import io.selorax.island.model.EventSource
import io.selorax.island.overlay.renderers.BatteryRenderer
import io.selorax.island.overlay.renderers.MusicRenderer
import io.selorax.island.overlay.renderers.NotificationRenderer
import io.selorax.island.overlay.renderers.TimerRenderer
import io.selorax.island.overlay.ui.RendererRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererRegistryTest {

    @Test
    fun get_media_returnsMusicRenderer() {
        val renderer = RendererRegistry.get(EventSource.MEDIA)
        assertNotNull(renderer)
        assertTrue(renderer is MusicRenderer)
    }

    @Test
    fun get_battery_returnsBatteryRenderer() {
        val renderer = RendererRegistry.get(EventSource.BATTERY)
        assertNotNull(renderer)
        assertTrue(renderer is BatteryRenderer)
    }

    @Test
    fun get_notification_returnsNotificationRenderer() {
        val renderer = RendererRegistry.get(EventSource.NOTIFICATION)
        assertNotNull(renderer)
        assertTrue(renderer is NotificationRenderer)
    }

    @Test
    fun get_timer_returnsTimerRenderer() {
        val renderer = RendererRegistry.get(EventSource.TIMER)
        assertNotNull(renderer)
        assertTrue(renderer is TimerRenderer)
    }

    @Test
    fun get_system_returnsNull() {
        val renderer = RendererRegistry.get(EventSource.SYSTEM)
        assertNull(renderer)
    }
}
