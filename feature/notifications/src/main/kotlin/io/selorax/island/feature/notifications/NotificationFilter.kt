package io.selorax.island.feature.notifications

public class NotificationFilter(
    private val ownPackageName: String
) {
    private data class CachedAlert(
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long
    )

    private val systemPackages = setOf(
        "com.android.systemui",
        "android",
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.systemui",
        "com.google.android.googlequicksearchbox"
    )

    private val chargingKeywords = listOf(
        "charging", "charger", "cable", "battery", "power", "super fast",
        "fully charged", "plugged", "usb", "connected", "screenshot",
        "debugging", "system update"
    )

    private val recentAlertsByKey = mutableMapOf<String, CachedAlert>()
    private var lastAcceptedAlert: CachedAlert? = null

    public fun shouldDisplay(
        snapshot: NotificationSnapshot,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        // 1. Ignore own package
        if (snapshot.packageName == ownPackageName) return false

        // 2. Ignore group summary notifications (e.g. "5 messages from 3 chats")
        if (snapshot.isGroupSummary) return false

        // 3. Ignore empty notifications (no title and no text)
        val title = snapshot.title?.trim() ?: ""
        val text = snapshot.text?.trim() ?: ""
        if (title.isEmpty() && text.isEmpty()) return false

        // 4. Ignore ongoing foreground service notifications (charging, downloads, pedometers, sync, music progress)
        // Exception: Phone calls and alarms MUST be displayed even if marked ongoing by the OS
        val isCallOrAlarm = snapshot.category == "call" || snapshot.category == "alarm"
        if (snapshot.isOngoing && !isCallOrAlarm) {
            return false
        }

        // 5. Ignore system UI & OS internal noise (charging notices, battery status, USB, screenshots)
        if (snapshot.packageName in systemPackages) {
            val contentLower = "$title $text".lowercase()
            if (chargingKeywords.any { contentLower.contains(it) }) {
                return false
            }
            if (snapshot.category in setOf("service", "status", "transport", "sys", "recommendation")) {
                return false
            }
            // General system UI notifications shouldn't spam unless they are calls
            if (!isCallOrAlarm) {
                return false
            }
        }

        // 6. Anti-spam & deduplication
        // Clean up expired keys older than 60 seconds
        recentAlertsByKey.entries.removeIf { currentTimeMs - it.value.timestamp > 60_000L }

        // Check if this exact notification key was already processed
        val previousByKey = recentAlertsByKey[snapshot.key]
        if (previousByKey != null) {
            // Same key, same title, same text within 30 seconds -> duplicate update (e.g. avatar or delivery status)
            if (previousByKey.title == title && previousByKey.text == text && currentTimeMs - previousByKey.timestamp < 30_000L) {
                return false
            }
            // Same key updated too rapidly (within 1.5 seconds) -> debounce rapid micro-updates
            if (currentTimeMs - previousByKey.timestamp < 1500L) {
                return false
            }
        }

        // Check if identical notification from same app was accepted very recently (e.g. dual notification or channel repeat)
        val last = lastAcceptedAlert
        if (last != null &&
            last.packageName == snapshot.packageName &&
            last.title == title &&
            last.text == text &&
            currentTimeMs - last.timestamp < 5000L
        ) {
            return false
        }

        // Record accepted alert
        val accepted = CachedAlert(
            packageName = snapshot.packageName,
            title = title,
            text = text,
            timestamp = currentTimeMs
        )
        recentAlertsByKey[snapshot.key] = accepted
        lastAcceptedAlert = accepted
        return true
    }

    public fun clearCache() {
        recentAlertsByKey.clear()
        lastAcceptedAlert = null
    }
}
