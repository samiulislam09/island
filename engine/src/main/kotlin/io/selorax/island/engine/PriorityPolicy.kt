package io.selorax.island.engine

import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EffectivePriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.LiveActivity

public class PriorityPolicy(
    public val minDisplayMs: Long = 2500L
) {
    public fun computePriority(
        activity: LiveActivity,
        currentEpochMs: Long,
        isExpanded: Boolean
    ): EffectivePriority {
        val base = when (activity.source) {
            EventSource.TIMER -> 90
            EventSource.BATTERY -> 85
            EventSource.MEDIA -> 70
            EventSource.NOTIFICATION -> 50
            EventSource.SYSTEM -> 40
        }

        val urgencyBoost = when (activity.priority) {
            ActivityPriority.CRITICAL -> 60
            ActivityPriority.HIGH -> 30
            ActivityPriority.DEFAULT -> 0
            ActivityPriority.LOW -> -20
        }

        val interactionBoost = if (isExpanded) 30 else 0

        // Staleness penalty: -1 per 10s up to -20
        val ageSeconds = (currentEpochMs - activity.updatedAt).coerceAtLeast(0L) / 1000L
        val staleness = (ageSeconds / 10L).toInt().coerceAtMost(20)

        val finalScore = base + urgencyBoost + interactionBoost - staleness
        return EffectivePriority(score = finalScore, isUrgent = activity.priority == ActivityPriority.CRITICAL)
    }

    public fun canPreempt(
        active: LiveActivity,
        newcomer: LiveActivity,
        currentEpochMs: Long
    ): Boolean {
        if (newcomer.priority == ActivityPriority.CRITICAL) return true
        val visibleSince = active.visibleSince ?: return true
        val displayTime = currentEpochMs - visibleSince
        return displayTime >= minDisplayMs
    }

    public fun rank(
        activities: List<LiveActivity>,
        currentEpochMs: Long,
        expandedKey: String?
    ): List<LiveActivity> {
        return activities.sortedByDescending { activity ->
            computePriority(activity, currentEpochMs, activity.key.raw == expandedKey).score
        }
    }
}
