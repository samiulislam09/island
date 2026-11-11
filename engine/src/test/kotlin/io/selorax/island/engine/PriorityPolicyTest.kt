package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity
import org.junit.jupiter.api.Test

class PriorityPolicyTest {
    private val policy = PriorityPolicy()

    private fun createActivity(
        key: String,
        source: EventSource,
        urgency: ActivityPriority = ActivityPriority.DEFAULT,
        visibleSince: Long = 0L,
        updatedAt: Long = 1000L
    ): LiveActivity {
        return LiveActivity(
            key = ActivityKey(key),
            source = source,
            priority = urgency,
            state = IslandState.Compact(IslandSlot.Icon("test"), IslandSlot.Text("label")),
            createdAt = 1000L,
            updatedAt = updatedAt,
            visibleSince = visibleSince
        )
    }

    @Test
    fun `urgent timer has higher priority than normal media`() {
        val timer = createActivity("timer:1", EventSource.TIMER, urgency = ActivityPriority.CRITICAL)
        val media = createActivity("media:spotify", EventSource.MEDIA, urgency = ActivityPriority.DEFAULT)

        val timerPriority = policy.computePriority(timer, currentEpochMs = 1000L, isExpanded = false)
        val mediaPriority = policy.computePriority(media, currentEpochMs = 1000L, isExpanded = false)

        assertThat(timerPriority.score).isGreaterThan(mediaPriority.score)
    }

    @Test
    fun `preemption guard blocks non-urgent newcomer before minDisplay elapsed`() {
        val active = createActivity("media:spotify", EventSource.MEDIA, visibleSince = 1000L)
        val newcomer = createActivity("notif:app:1", EventSource.NOTIFICATION, urgency = ActivityPriority.DEFAULT)

        // Only 1000ms elapsed since active became visible (minDisplay is 2500ms)
        val canPreempt = policy.canPreempt(active, newcomer, currentEpochMs = 2000L)
        assertThat(canPreempt).isFalse()
    }

    @Test
    fun `preemption guard allows urgent newcomer before minDisplay elapsed`() {
        val active = createActivity("media:spotify", EventSource.MEDIA, visibleSince = 1000L)
        val urgentTimer = createActivity("timer:1", EventSource.TIMER, urgency = ActivityPriority.CRITICAL)

        val canPreempt = policy.canPreempt(active, urgentTimer, currentEpochMs = 2000L)
        assertThat(canPreempt).isTrue()
    }
}
