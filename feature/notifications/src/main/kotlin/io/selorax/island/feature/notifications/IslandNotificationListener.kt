package io.selorax.island.feature.notifications

import android.app.Notification
import android.content.Intent
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

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: extras.getCharSequence("android.title")?.toString()
            ?: ""

        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: (extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.lastOrNull()?.toString())
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?: notification.tickerText?.toString()
            ?: ""

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        val finalTitle = if (rawTitle.isNotBlank()) rawTitle else appName
        val finalText = if (rawText.isNotBlank()) rawText else "New notification"

        val snapshot = NotificationSnapshot(
            key = sbn.key ?: return,
            packageName = sbn.packageName ?: return,
            postTime = sbn.postTime,
            title = finalTitle,
            text = finalText,
            isOngoing = sbn.isOngoing,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            category = notification.category
        )

        if (filter.shouldDisplay(snapshot)) {
            // Direct zero-latency in-process callback is primary
            val inProcessCallback = IslandNotificationHub.onNotificationReceived
            if (inProcessCallback != null) {
                inProcessCallback.invoke(finalTitle, finalText, snapshot.packageName)
            } else {
                // Fallback when running across process boundaries
                val alert = mapper.mapToAlert(snapshot)
                bridge.emit(alert)
                try {
                    val intent = Intent(this, Class.forName("io.selorax.island.overlay.OverlayHost")).apply {
                        action = "io.selorax.island.overlay.SHOW_NOTIFICATION"
                        putExtra("extra_title", finalTitle)
                        putExtra("extra_text", finalText)
                        putExtra("extra_package", snapshot.packageName)
                    }
                    startService(intent)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        val packageName = sbn.packageName ?: return
        val event = mapper.mapToEnd(key, packageName)
        bridge.emit(event)
    }
}
