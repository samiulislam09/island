package io.selorax.island.feature.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class NotificationFilterTest {
    private val filter = NotificationFilter(ownPackageName = "io.selorax.island")

    @Before
    fun setUp() {
        filter.clearCache()
    }

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

    @Test
    fun `rejects ongoing notifications from regular apps`() {
        val ongoingDownload = NotificationSnapshot(
            key = "download_key",
            packageName = "com.android.providers.downloads",
            postTime = 100L,
            title = "Downloading file",
            text = "45% complete",
            isOngoing = true,
            isGroupSummary = false,
            category = "progress"
        )
        assertThat(filter.shouldDisplay(ongoingDownload)).isFalse()
    }

    @Test
    fun `accepts ongoing call notification`() {
        val callSnapshot = NotificationSnapshot(
            key = "call_key",
            packageName = "com.google.android.dialer",
            postTime = 100L,
            title = "Incoming Call",
            text = "Mom",
            isOngoing = true,
            isGroupSummary = false,
            category = "call"
        )
        assertThat(filter.shouldDisplay(callSnapshot)).isTrue()
    }

    @Test
    fun `rejects system UI charging notification`() {
        val chargingSnapshot = NotificationSnapshot(
            key = "system_charging_key",
            packageName = "com.android.systemui",
            postTime = 100L,
            title = "Fast charging",
            text = "72% • 35m until full",
            isOngoing = true,
            isGroupSummary = false,
            category = "status"
        )
        assertThat(filter.shouldDisplay(chargingSnapshot)).isFalse()
    }

    @Test
    fun `rejects duplicate notification updates with identical content`() {
        val initialMessage = NotificationSnapshot(
            key = "msg_key_1",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Sarah",
            text = "Hey dinner tonight?",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )
        val duplicateUpdate = NotificationSnapshot(
            key = "msg_key_1",
            packageName = "com.whatsapp",
            postTime = 200L,
            title = "Sarah",
            text = "Hey dinner tonight?",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )

        // First message must be accepted
        assertThat(filter.shouldDisplay(initialMessage, currentTimeMs = 1000L)).isTrue()

        // Immediate duplicate update for same key must be rejected
        assertThat(filter.shouldDisplay(duplicateUpdate, currentTimeMs = 1500L)).isFalse()
    }

    @Test
    fun `accepts new message with different text from same sender`() {
        val message1 = NotificationSnapshot(
            key = "msg_key_2",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Sarah",
            text = "First message",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )
        val message2 = NotificationSnapshot(
            key = "msg_key_2",
            packageName = "com.whatsapp",
            postTime = 5000L,
            title = "Sarah",
            text = "Second message",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )

        assertThat(filter.shouldDisplay(message1, currentTimeMs = 1000L)).isTrue()
        assertThat(filter.shouldDisplay(message2, currentTimeMs = 6000L)).isTrue()
    }

    @Test
    fun `rejects empty title and empty text notification`() {
        val emptySnapshot = NotificationSnapshot(
            key = "empty_key",
            packageName = "com.example.app",
            postTime = 100L,
            title = "   ",
            text = "",
            isOngoing = false,
            isGroupSummary = false,
            category = null
        )
        assertThat(filter.shouldDisplay(emptySnapshot)).isFalse()
    }
}
