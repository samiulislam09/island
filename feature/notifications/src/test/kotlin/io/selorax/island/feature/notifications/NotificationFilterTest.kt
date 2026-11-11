package io.selorax.island.feature.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationFilterTest {
    private val filter = NotificationFilter(ownPackageName = "io.selorax.island")

    @Test
    fun `rejects notification from own package`() {
        val snapshot = NotificationSnapshot(
            key = "key1",
            packageName = "io.selorax.island",
            postTime = 100L,
            title = "Test",
            text = "Body",
            isOngoing = false,
            isGroupSummary = false,
            category = null
        )
        assertThat(filter.shouldDisplay(snapshot)).isFalse()
    }

    @Test
    fun `rejects group summary notification`() {
        val snapshot = NotificationSnapshot(
            key = "key2",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Group",
            text = "5 messages",
            isOngoing = false,
            isGroupSummary = true,
            category = null
        )
        assertThat(filter.shouldDisplay(snapshot)).isFalse()
    }

    @Test
    fun `accepts regular message notification`() {
        val snapshot = NotificationSnapshot(
            key = "key3",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Alice",
            text = "See you soon",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )
        assertThat(filter.shouldDisplay(snapshot)).isTrue()
    }
}
