package io.selorax.island.feature.notifications

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandAction
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints

public class NotificationMapper {
    public fun mapToUpsert(snapshot: NotificationSnapshot): IslandEvent.Upsert {
        val title = snapshot.title ?: "Notification"
        val text = snapshot.text ?: ""

        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon(snapshot.packageName),
            trailingSlot = IslandSlot.Text(title.take(12)),
            centerContent = null
        )

        return IslandEvent.Upsert(
            key = ActivityKey("notif:${snapshot.packageName}:${snapshot.key}"),
            source = EventSource.NOTIFICATION,
            timestamp = snapshot.postTime,
            state = state,
            hints = PriorityHints()
        )
    }

    public fun mapToAlert(snapshot: NotificationSnapshot, durationMs: Long = 4500L): IslandEvent.Alert {
        val title = snapshot.title ?: "Notification"
        val text = snapshot.text ?: ""
        val state = IslandState.Expanded(
            title = title,
            subtitle = text.ifEmpty { null },
            leadingSlot = IslandSlot.Icon(snapshot.packageName),
            contentSlot = null,
            actions = listOf(
                IslandAction("dismiss", "Dismiss"),
                IslandAction("open", "Open")
            )
        )
        return IslandEvent.Alert(
            key = ActivityKey("notif:${snapshot.packageName}:${snapshot.key}"),
            source = EventSource.NOTIFICATION,
            timestamp = snapshot.postTime,
            state = state,
            durationMs = durationMs
        )
    }

    public fun mapToEnd(key: String, packageName: String): IslandEvent.End {
        return IslandEvent.End(
            key = ActivityKey("notif:$packageName:$key"),
            source = EventSource.NOTIFICATION,
            timestamp = System.currentTimeMillis()
        )
    }
}
