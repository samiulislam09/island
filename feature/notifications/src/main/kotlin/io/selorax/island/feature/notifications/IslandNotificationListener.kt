package io.selorax.island.feature.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

public class IslandNotificationListener : NotificationListenerService() {
    public companion object {
        public val bridge: NotificationEventBridge = NotificationEventBridge()
    }

    private lateinit var filter: NotificationFilter
    private val mapper: NotificationMapper = NotificationMapper()

    override fun onCreate() {
        super.onCreate()
        filter = NotificationFilter(ownPackageName = packageName)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return

        val snapshot = NotificationSnapshot(
            key = sbn.key ?: return,
            packageName = sbn.packageName ?: return,
            postTime = sbn.postTime,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            isOngoing = sbn.isOngoing,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            category = notification.category
        )

        if (filter.shouldDisplay(snapshot)) {
            val event = mapper.mapToUpsert(snapshot)
            bridge.emit(event)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        val packageName = sbn.packageName ?: return
        val event = mapper.mapToEnd(key, packageName)
        bridge.emit(event)
    }
}
