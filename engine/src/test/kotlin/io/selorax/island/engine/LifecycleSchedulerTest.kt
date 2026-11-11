package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LifecycleSchedulerTest {
    private val scheduler = LifecycleScheduler()

    @Test
    fun `calculates earliest deadline between auto-collapse and minDisplay`() {
        val deadlines = listOf(5000L, 8000L, 3000L)
        val nearest = scheduler.earliestDeadline(deadlines, currentEpochMs = 1000L)

        assertThat(nearest).isEqualTo(3000L)
    }

    @Test
    fun `ignores past deadlines`() {
        val deadlines = listOf(500L, 800L, 3000L)
        val nearest = scheduler.earliestDeadline(deadlines, currentEpochMs = 1000L)

        assertThat(nearest).isEqualTo(3000L)
    }
}
