package io.selorax.island.engine

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.LiveActivity

public class ActivityRegistry(
    private val maxItems: Int = 20
) {
    private val items = mutableMapOf<ActivityKey, LiveActivity>()

    public val size: Int get() = items.size

    public fun get(key: ActivityKey): LiveActivity? = items[key]

    public fun getAll(): List<LiveActivity> = items.values.toList()

    public fun upsert(activity: LiveActivity) {
        if (!items.containsKey(activity.key) && items.size >= maxItems) {
            // Evict oldest by updatedAt
            val oldestKey = items.minByOrNull { it.value.updatedAt }?.key
            if (oldestKey != null) {
                items.remove(oldestKey)
            }
        }
        items[activity.key] = activity
    }

    public fun remove(key: ActivityKey): LiveActivity? = items.remove(key)

    public fun clear() {
        items.clear()
    }
}
