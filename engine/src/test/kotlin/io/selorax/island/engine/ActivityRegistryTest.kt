package io.selorax.island.engine

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity
import org.junit.jupiter.api.Test

class ActivityRegistryTest {
    private val registry = ActivityRegistry(maxItems = 3)

    private fun create(key: String, updatedAt: Long): LiveActivity {
        return LiveActivity(
            key = ActivityKey(key),
            source = EventSource.NOTIFICATION,
            state = IslandState.Compact(IslandSlot.Icon("icon"), IslandSlot.Text(key)),
            createdAt = updatedAt,
            updatedAt = updatedAt
        )
    }

    @Test
    fun `upsert updates existing item`() {
        registry.upsert(create("1", 100L))
        registry.upsert(create("1", 200L))

        assertThat(registry.size).isEqualTo(1)
        assertThat(registry.get(ActivityKey("1"))?.updatedAt).isEqualTo(200L)
    }

    @Test
    fun `capacity eviction removes oldest item`() {
        registry.upsert(create("1", 100L))
        registry.upsert(create("2", 200L))
        registry.upsert(create("3", 300L))
        registry.upsert(create("4", 400L))

        assertThat(registry.size).isEqualTo(3)
        assertThat(registry.get(ActivityKey("1"))).isNull()
        assertThat(registry.get(ActivityKey("4"))).isNotNull()
    }
}
