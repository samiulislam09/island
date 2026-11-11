package io.selorax.island.feature.notifications

public class NotificationFilter(
    private val ownPackageName: String
) {
    public fun shouldDisplay(snapshot: NotificationSnapshot): Boolean {
        if (snapshot.packageName == ownPackageName) return false
        if (snapshot.isGroupSummary) return false
        if (snapshot.isOngoing) return false
        if (snapshot.category == "transport" || snapshot.category == "service") return false
        if (snapshot.title.isNullOrBlank() && snapshot.text.isNullOrBlank()) return false
        return true
    }
}
