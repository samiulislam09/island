package io.selorax.island.engine

public class LifecycleScheduler {
    public fun earliestDeadline(
        deadlines: List<Long>,
        currentEpochMs: Long
    ): Long? {
        return deadlines
            .filter { it > currentEpochMs }
            .minOrNull()
    }
}
