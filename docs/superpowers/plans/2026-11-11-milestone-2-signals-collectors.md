# Milestone 2 (Signals & Event Collectors) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement normalized event contracts in `:core:model`, permissions monitoring in `:core:permissions`, typed DataStore & Room persistence in `:core:data`, and the system signal collectors for notifications, media, battery, and timers across `feature:*` modules.

**Architecture:** All collectors implement the `EventCollector` interface and emit normalized `IslandEvent` flows via `callbackFlow` or channels. Platform types never cross domain boundaries. Settings are persisted via typed `DataStore<IslandSettings>` with JSON serialization; rules and timers are stored in Room v1. `IslandNotificationListenerService` serves as the system-bound background anchor.

**Tech Stack:** Kotlin 2.1.10, Coroutines 1.10.1, Jetpack DataStore 1.1.2, Room 2.6.1, KSP 2.1.10-1.0.30, JUnit 5 / JUnit 4, Truth, MockK, Turbine.

**Spec:** [`docs/superpowers/specs/2026-11-11-milestone-2-signals-collectors-design.md`](file:///Volumes/work/personal/dynamic-island/docs/superpowers/specs/2026-11-11-milestone-2-signals-collectors-design.md)

## Global Constraints

- No notification text, titles, contact names, or album artwork may ever be persisted to disk (ADR-07).
- No network access or `INTERNET` permission in any manifest.
- Do not commit automatically to git (ruling from user).
- Android API: `minSdk = 29`, `compileSdk = 36`, `targetSdk = 36`, JVM toolchain = 21.

## Review Focus

1. Corrupted DataStore file: `IslandSettingsSerializer` must gracefully fall back to default settings without crashing the app.
2. High-frequency battery churn: `BatteryCollector` must not emit redundant alerts on minor voltage or temperature fluctuations.
3. Notification group summaries: `NotificationFilter` must drop group summary notifications so child messages are displayed cleanly.
4. Media artwork memory: `MediaSessionCollector` must downscale album artwork to ≤ 256 px to respect NFR-04 memory limits.
5. Notification Listener disconnect: `IslandNotificationListenerService` must handle unexpected unbind without leaving corrupted bridge states.

---

### Task 1: Version Catalog & Persistence Dependencies Setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `core/data/build.gradle.kts`
- Modify: `build.gradle.kts`

**Interfaces:**
- Consumes: Version catalog
- Produces: DataStore, Room, and KSP dependencies configured in `:core:data`

- [ ] **Step 1: Add DataStore and Room libraries to `gradle/libs.versions.toml`**

Add to `[versions]`:
```toml
datastore = "1.1.2"
room = "2.6.1"
ksp = "2.1.10-1.0.30"
```

Add to `[libraries]`:
```toml
androidx-datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

Add to `[plugins]`:
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Update `core/data/build.gradle.kts`**

```kotlin
plugins {
    id("island.android.library")
    id("island.test")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

- [ ] **Step 3: Verify `:core:data` compiles**

Run:
```bash
./gradlew :core:data:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 2: Normalized Event Domain Models (`:core:model` & `:core:common`)

**Files:**
- Create: `core/model/src/main/kotlin/io/selorax/island/model/IslandEvent.kt`
- Create: `core/model/src/main/kotlin/io/selorax/island/model/SystemEvent.kt`
- Create: `core/common/src/main/kotlin/io/selorax/island/common/EventCollector.kt`
- Test: `core/model/src/test/kotlin/io/selorax/island/model/IslandEventTest.kt`

**Interfaces:**
- Consumes: `IslandState`
- Produces: `IslandEvent`, `SystemEvent`, `EventSource`, `ActivityKey`, `PriorityHints`, `EventCollector`

- [ ] **Step 1: Write failing unit test `IslandEventTest`**

`core/model/src/test/kotlin/io/selorax/island/model/IslandEventTest.kt`:
```kotlin
package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IslandEventTest {
    @Test
    fun `upsert event carries key source and state`() {
        val event = IslandEvent.Upsert(
            key = ActivityKey("notif:test:1"),
            source = EventSource.NOTIFICATION,
            timestamp = 1000L,
            state = IslandState.Compact(
                leadingSlot = IslandSlot.Icon("msg"),
                trailingSlot = IslandSlot.Text("Hello")
            ),
            hints = PriorityHints(priority = ActivityPriority.HIGH)
        )

        assertThat(event.key.raw).isEqualTo("notif:test:1")
        assertThat(event.source).isEqualTo(EventSource.NOTIFICATION)
        assertThat(event.hints.priority).isEqualTo(ActivityPriority.HIGH)
    }

    @Test
    fun `end event carries key and optional reason`() {
        val event = IslandEvent.End(
            key = ActivityKey("media:spotify"),
            source = EventSource.MEDIA,
            timestamp = 2000L,
            reason = "playback_stopped"
        )

        assertThat(event.key.raw).isEqualTo("media:spotify")
        assertThat(event.reason).isEqualTo("playback_stopped")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:model:test
```
Expected: FAIL (unresolved references `IslandEvent`, `EventSource`, `ActivityKey`, `PriorityHints`).

- [ ] **Step 3: Implement `IslandEvent`, `SystemEvent`, and `EventCollector`**

`core/model/src/main/kotlin/io/selorax/island/model/IslandEvent.kt`:
```kotlin
package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public enum class EventSource {
    NOTIFICATION,
    MEDIA,
    BATTERY,
    TIMER,
    SYSTEM
}

@Serializable
public data class ActivityKey(public val raw: String)

@Serializable
public data class PriorityHints(
    public val priority: ActivityPriority = ActivityPriority.DEFAULT,
    public val isUrgent: Boolean = false
)

@Serializable
public sealed interface IslandEvent {
    public val key: ActivityKey
    public val source: EventSource
    public val timestamp: Long

    @Serializable
    public data class Upsert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val hints: PriorityHints = PriorityHints()
    ) : IslandEvent

    @Serializable
    public data class End(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val reason: String? = null
    ) : IslandEvent

    @Serializable
    public data class Alert(
        override val key: ActivityKey,
        override val source: EventSource,
        override val timestamp: Long,
        public val state: IslandState,
        public val durationMs: Long
    ) : IslandEvent
}
```

`core/model/src/main/kotlin/io/selorax/island/model/SystemEvent.kt`:
```kotlin
package io.selorax.island.model

import kotlinx.serialization.Serializable

@Serializable
public sealed interface SystemEvent {
    @Serializable
    public data class ScreenState(public val isInteractive: Boolean, public val isLocked: Boolean) : SystemEvent

    @Serializable
    public data class Orientation(public val isLandscape: Boolean) : SystemEvent

    @Serializable
    public data class PermissionsChanged(public val hasOverlay: Boolean, public val hasNotificationAccess: Boolean) : SystemEvent
}
```

`core/common/src/main/kotlin/io/selorax/island/common/EventCollector.kt`:
```kotlin
package io.selorax.island.common

import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow

public interface EventCollector {
    public val source: EventSource
    public val events: Flow<IslandEvent>
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :core:model:test :core:common:compileKotlin
```
Expected: PASS.

---

### Task 3: Permissions Monitoring Subsystem (`:core:permissions`)

**Files:**
- Create: `core/permissions/src/main/kotlin/io/selorax/island/permissions/PermissionSnapshot.kt`
- Create: `core/permissions/src/main/kotlin/io/selorax/island/permissions/PermissionMonitor.kt`
- Test: `core/permissions/src/test/kotlin/io/selorax/island/permissions/PermissionMonitorTest.kt`

**Interfaces:**
- Consumes: Android `Context`
- Produces: `PermissionSnapshot`, `PermissionMonitor` exposing `StateFlow<PermissionSnapshot>`

- [ ] **Step 1: Write failing unit test `PermissionMonitorTest`**

`core/permissions/src/test/kotlin/io/selorax/island/permissions/PermissionMonitorTest.kt`:
```kotlin
package io.selorax.island.permissions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionMonitorTest {
    @Test
    fun `snapshot reports complete permissions when all granted`() {
        val snapshot = PermissionSnapshot(
            hasOverlayPermission = true,
            hasNotificationAccess = true,
            canScheduleExactAlarms = true
        )

        assertThat(snapshot.isFullyOperational).isTrue()
    }

    @Test
    fun `snapshot reports incomplete when overlay permission missing`() {
        val snapshot = PermissionSnapshot(
            hasOverlayPermission = false,
            hasNotificationAccess = true,
            canScheduleExactAlarms = true
        )

        assertThat(snapshot.isFullyOperational).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:permissions:testDebugUnitTest
```
Expected: FAIL (unresolved class `PermissionSnapshot`).

- [ ] **Step 3: Implement `PermissionSnapshot` and `PermissionMonitor`**

`core/permissions/src/main/kotlin/io/selorax/island/permissions/PermissionSnapshot.kt`:
```kotlin
package io.selorax.island.permissions

public data class PermissionSnapshot(
    public val hasOverlayPermission: Boolean,
    public val hasNotificationAccess: Boolean,
    public val canScheduleExactAlarms: Boolean
) {
    public val isFullyOperational: Boolean
        get() = hasOverlayPermission && hasNotificationAccess
}
```

`core/permissions/src/main/kotlin/io/selorax/island/permissions/PermissionMonitor.kt`:
```kotlin
package io.selorax.island.permissions

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class PermissionMonitor(
    private val context: Context
) {
    private val _permissions = MutableStateFlow(checkPermissions())
    public val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    public fun checkPermissions(): PermissionSnapshot {
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

        val snapshot = PermissionSnapshot(
            hasOverlayPermission = hasOverlay,
            hasNotificationAccess = hasNotificationAccess,
            canScheduleExactAlarms = canExactAlarm
        )
        _permissions.value = snapshot
        return snapshot
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :core:permissions:testDebugUnitTest
```
Expected: PASS.

---

### Task 4: DataStore Settings Persistence (`:core:data`)

**Files:**
- Create: `core/data/src/main/kotlin/io/selorax/island/data/settings/IslandSettings.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/settings/IslandSettingsSerializer.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/settings/SettingsRepository.kt`
- Test: `core/data/src/test/kotlin/io/selorax/island/data/settings/IslandSettingsSerializerTest.kt`

**Interfaces:**
- Consumes: Jetpack DataStore
- Produces: `IslandSettings`, `SettingsRepository` exposing `Flow<IslandSettings>`

- [ ] **Step 1: Write failing unit test `IslandSettingsSerializerTest`**

`core/data/src/test/kotlin/io/selorax/island/data/settings/IslandSettingsSerializerTest.kt`:
```kotlin
package io.selorax.island.data.settings

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class IslandSettingsSerializerTest {
    private val serializer = IslandSettingsSerializer()

    @Test
    fun `serializer round trip preserves values`() = runBlocking {
        val original = IslandSettings(
            islandEnabled = true,
            anchor = "BELOW_STATUS_BAR",
            hideInLandscape = false,
            onboardingCompleted = true
        )

        val output = ByteArrayOutputStream()
        serializer.writeTo(original, output)

        val input = ByteArrayInputStream(output.toByteArray())
        val restored = serializer.readFrom(input)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `corrupted stream returns defaultValue`() = runBlocking {
        val corrupted = ByteArrayInputStream("not valid json".toByteArray())
        val restored = serializer.readFrom(corrupted)

        assertThat(restored.schemaVersion).isEqualTo(1)
        assertThat(restored.islandEnabled).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:data:testDebugUnitTest
```
Expected: FAIL (unresolved class `IslandSettingsSerializer`).

- [ ] **Step 3: Implement `IslandSettings`, `IslandSettingsSerializer`, and `SettingsRepository`**

`core/data/src/main/kotlin/io/selorax/island/data/settings/IslandSettings.kt`:
```kotlin
package io.selorax.island.data.settings

import kotlinx.serialization.Serializable

@Serializable
public data class IslandSettings(
    public val schemaVersion: Int = 1,
    public val islandEnabled: Boolean = true,
    public val anchor: String = "CUTOUT",
    public val compactWidthDp: Float = 160f,
    public val compactHeightDp: Float = 38f,
    public val hideInLandscape: Boolean = true,
    public val onboardingCompleted: Boolean = false,
    public val notificationDisclosureAccepted: Boolean = false
)
```

`core/data/src/main/kotlin/io/selorax/island/data/settings/IslandSettingsSerializer.kt`:
```kotlin
package io.selorax.island.data.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

public class IslandSettingsSerializer(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : Serializer<IslandSettings> {
    override val defaultValue: IslandSettings = IslandSettings()

    override suspend fun readFrom(input: InputStream): IslandSettings {
        return try {
            val text = input.bufferedReader().use { it.readText() }
            if (text.isBlank()) defaultValue else json.decodeFromString<IslandSettings>(text)
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: IslandSettings, output: OutputStream) {
        val text = json.encodeToString(IslandSettings.serializer(), t)
        output.write(text.toByteArray())
    }
}
```

`core/data/src/main/kotlin/io/selorax/island/data/settings/SettingsRepository.kt`:
```kotlin
package io.selorax.island.data.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

public class SettingsRepository(
    private val dataStore: DataStore<IslandSettings>
) {
    public val settings: Flow<IslandSettings> = dataStore.data

    public suspend fun updateIslandEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(islandEnabled = enabled) }
    }

    public suspend fun updateAnchor(anchor: String) {
        dataStore.updateData { it.copy(anchor = anchor) }
    }

    public suspend fun completeOnboarding() {
        dataStore.updateData { it.copy(onboardingCompleted = true) }
    }

    public suspend fun acceptNotificationDisclosure() {
        dataStore.updateData { it.copy(notificationDisclosureAccepted = true) }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :core:data:testDebugUnitTest
```
Expected: PASS.

---

### Task 5: Room Database v1 (`:core:data`)

**Files:**
- Create: `core/data/src/main/kotlin/io/selorax/island/data/db/AppNotificationRule.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/db/AppRuleDao.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/db/TimerEntity.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/db/TimerDao.kt`
- Create: `core/data/src/main/kotlin/io/selorax/island/data/db/IslandDatabase.kt`

**Interfaces:**
- Consumes: Room KSP compiler
- Produces: `IslandDatabase`, `AppRuleDao`, `TimerDao`

- [ ] **Step 1: Implement entities and DAOs in `core/data`**

`core/data/src/main/kotlin/io/selorax/island/data/db/AppNotificationRule.kt`:
```kotlin
package io.selorax.island.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notification_rules")
public data class AppNotificationRule(
    @PrimaryKey public val packageName: String,
    public val enabled: Boolean = true,
    public val showPreview: Boolean = true,
    public val updatedAt: Long
)
```

`core/data/src/main/kotlin/io/selorax/island/data/db/AppRuleDao.kt`:
```kotlin
package io.selorax.island.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public interface AppRuleDao {
    @Query("SELECT * FROM app_notification_rules WHERE packageName = :packageName")
    public suspend fun getRule(packageName: String): AppNotificationRule?

    @Query("SELECT * FROM app_notification_rules")
    public fun getAllRules(): Flow<List<AppNotificationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun upsertRule(rule: AppNotificationRule)

    @Query("DELETE FROM app_notification_rules WHERE packageName = :packageName")
    public suspend fun deleteRule(packageName: String)
}
```

`core/data/src/main/kotlin/io/selorax/island/data/db/TimerEntity.kt`:
```kotlin
package io.selorax.island.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
public data class TimerEntity(
    @PrimaryKey public val id: String,
    public val label: String,
    public val targetEpochMs: Long,
    public val originalDurationMs: Long,
    public val isRunning: Boolean,
    public val remainingPauseMs: Long? = null,
    public val createdAt: Long
)
```

`core/data/src/main/kotlin/io/selorax/island/data/db/TimerDao.kt`:
```kotlin
package io.selorax.island.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public interface TimerDao {
    @Query("SELECT * FROM timers WHERE isRunning = 1")
    public fun getRunningTimers(): Flow<List<TimerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun upsertTimer(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    public suspend fun deleteTimer(id: String)
}
```

`core/data/src/main/kotlin/io/selorax/island/data/db/IslandDatabase.kt`:
```kotlin
package io.selorax.island.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppNotificationRule::class, TimerEntity::class],
    version = 1,
    exportSchema = false
)
public abstract class IslandDatabase : RoomDatabase() {
    public abstract fun appRuleDao(): AppRuleDao
    public abstract fun timerDao(): TimerDao
}
```

- [ ] **Step 2: Verify Room compiles with KSP**

Run:
```bash
./gradlew :core:data:assembleDebug
```
Expected: BUILD SUCCESSFUL (KSP generates Room implementation).

---

### Task 6: Notifications Filter & Mapper (`:feature:notifications`)

**Files:**
- Create: `feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationSnapshot.kt`
- Create: `feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationFilter.kt`
- Create: `feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationMapper.kt`
- Test: `feature/notifications/src/test/kotlin/io/selorax/island/feature/notifications/NotificationFilterTest.kt`

**Interfaces:**
- Consumes: `NotificationSnapshot`
- Produces: Filtered snapshots mapped to `IslandEvent.Upsert`

- [ ] **Step 1: Write failing unit test `NotificationFilterTest`**

`feature/notifications/src/test/kotlin/io/selorax/island/feature/notifications/NotificationFilterTest.kt`:
```kotlin
package io.selorax.island.feature.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationFilterTest {
    private val filter = NotificationFilter(ownPackageName = "io.selorax.island")

    @Test
    fun `rejects notification from own package`() {
        val snapshot = NotificationSnapshot(
            key = "key1",
            packageName = "io.selorax.island",
            postTime = 100L,
            title = "Test",
            text = "Body",
            isOngoing = false,
            isGroupSummary = false,
            category = null
        )
        assertThat(filter.shouldDisplay(snapshot)).isFalse()
    }

    @Test
    fun `rejects group summary notification`() {
        val snapshot = NotificationSnapshot(
            key = "key2",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Group",
            text = "5 messages",
            isOngoing = false,
            isGroupSummary = true,
            category = null
        )
        assertThat(filter.shouldDisplay(snapshot)).isFalse()
    }

    @Test
    fun `accepts regular message notification`() {
        val snapshot = NotificationSnapshot(
            key = "key3",
            packageName = "com.whatsapp",
            postTime = 100L,
            title = "Alice",
            text = "See you soon",
            isOngoing = false,
            isGroupSummary = false,
            category = "msg"
        )
        assertThat(filter.shouldDisplay(snapshot)).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :feature:notifications:testDebugUnitTest
```
Expected: FAIL (unresolved classes).

- [ ] **Step 3: Implement `NotificationSnapshot`, `NotificationFilter`, and `NotificationMapper`**

`feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationSnapshot.kt`:
```kotlin
package io.selorax.island.feature.notifications

public data class NotificationSnapshot(
    public val key: String,
    public val packageName: String,
    public val postTime: Long,
    public val title: String?,
    public val text: String?,
    public val isOngoing: Boolean,
    public val isGroupSummary: Boolean,
    public val category: String?
)
```

`feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationFilter.kt`:
```kotlin
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
```

`feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationMapper.kt`:
```kotlin
package io.selorax.island.feature.notifications

import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
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

    public fun mapToEnd(key: String, packageName: String): IslandEvent.End {
        return IslandEvent.End(
            key = ActivityKey("notif:$packageName:$key"),
            source = EventSource.NOTIFICATION,
            timestamp = System.currentTimeMillis()
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :feature:notifications:testDebugUnitTest
```
Expected: PASS.

---

### Task 7: Notification Listener Service & Event Bridge (`:feature:notifications`)

**Files:**
- Create: `feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationEventBridge.kt`
- Create: `feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/IslandNotificationListener.kt`
- Create: `feature/notifications/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Android `NotificationListenerService`
- Produces: `EventCollector` emitting `IslandEvent` from system notifications

- [ ] **Step 1: Implement `NotificationEventBridge`**

`feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/NotificationEventBridge.kt`:
```kotlin
package io.selorax.island.feature.notifications

import io.selorax.island.common.EventCollector
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class NotificationEventBridge : EventCollector {
    override val source: EventSource = EventSource.NOTIFICATION

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 64)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun emit(event: IslandEvent) {
        _events.tryEmit(event)
    }
}
```

- [ ] **Step 2: Implement `IslandNotificationListener`**

`feature/notifications/src/main/kotlin/io/selorax/island/feature/notifications/IslandNotificationListener.kt`:
```kotlin
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
```

- [ ] **Step 3: Register in `feature/notifications/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <service
            android:name=".IslandNotificationListener"
            android:label="Island Notification Listener"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

- [ ] **Step 4: Verify `:feature:notifications` compiles**

Run:
```bash
./gradlew :feature:notifications:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 8: Media Session Collector (`:feature:media`)

**Files:**
- Create: `feature/media/src/main/kotlin/io/selorax/island/feature/media/MediaSessionCollector.kt`
- Test: `feature/media/src/test/kotlin/io/selorax/island/feature/media/MediaSessionCollectorTest.kt`

**Interfaces:**
- Consumes: Android `MediaSessionManager`
- Produces: `EventCollector` emitting `IslandEvent` for active playback

- [ ] **Step 1: Implement `MediaSessionCollector`**

`feature/media/src/main/kotlin/io/selorax/island/feature/media/MediaSessionCollector.kt`:
```kotlin
package io.selorax.island.feature.media

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class MediaSessionCollector : EventCollector {
    override val source: EventSource = EventSource.MEDIA

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 32)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun onPlaybackStarted(packageName: String, title: String, artist: String) {
        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("music"),
            trailingSlot = IslandSlot.Text(title.take(12)),
            centerContent = null
        )
        val event = IslandEvent.Upsert(
            key = ActivityKey("media:$packageName"),
            source = EventSource.MEDIA,
            timestamp = System.currentTimeMillis(),
            state = state,
            hints = PriorityHints()
        )
        _events.tryEmit(event)
    }

    public fun onPlaybackStopped(packageName: String) {
        val event = IslandEvent.End(
            key = ActivityKey("media:$packageName"),
            source = EventSource.MEDIA,
            timestamp = System.currentTimeMillis(),
            reason = "playback_paused"
        )
        _events.tryEmit(event)
    }
}
```

- [ ] **Step 2: Write unit test `MediaSessionCollectorTest`**

`feature/media/src/test/kotlin/io/selorax/island/feature/media/MediaSessionCollectorTest.kt`:
```kotlin
package io.selorax.island.feature.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MediaSessionCollectorTest {
    private val collector = MediaSessionCollector()

    @Test
    fun `playback start emits upsert event`() = runBlocking {
        collector.events.test {
            collector.onPlaybackStarted("com.spotify", "Blinding Lights", "The Weeknd")

            val event = awaitItem() as IslandEvent.Upsert
            assertThat(event.key.raw).isEqualTo("media:com.spotify")
            assertThat(event.state.isVisible).isTrue()
        }
    }

    @Test
    fun `playback stop emits end event`() = runBlocking {
        collector.events.test {
            collector.onPlaybackStopped("com.spotify")

            val event = awaitItem() as IslandEvent.End
            assertThat(event.key.raw).isEqualTo("media:com.spotify")
            assertThat(event.reason).isEqualTo("playback_paused")
        }
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

Run:
```bash
./gradlew :feature:media:testDebugUnitTest
```
Expected: PASS.

---

### Task 9: Battery Events Collector (`:feature:battery`)

**Files:**
- Create: `feature/battery/src/main/kotlin/io/selorax/island/feature/battery/BatteryCollector.kt`
- Test: `feature/battery/src/test/kotlin/io/selorax/island/feature/battery/BatteryCollectorTest.kt`

**Interfaces:**
- Consumes: Battery state updates
- Produces: `EventCollector` emitting `IslandEvent.Alert` on charge connect and low/full thresholds

- [ ] **Step 1: Implement `BatteryCollector`**

`feature/battery/src/main/kotlin/io/selorax/island/feature/battery/BatteryCollector.kt`:
```kotlin
package io.selorax.island.feature.battery

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.ActivityPriority
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class BatteryCollector : EventCollector {
    override val source: EventSource = EventSource.BATTERY

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 16)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    private var lastPluggedState: Boolean? = null
    private var lastLevelAlerted: Int? = null

    public fun onBatteryStateChanged(level: Int, isCharging: Boolean) {
        // Charging plug-in alert
        if (isCharging && lastPluggedState != true) {
            emitAlert("Charging $level%", durationMs = 3000L)
        } else if (!isCharging && lastPluggedState == true) {
            emitAlert("Unplugged", durationMs = 2000L)
        }

        // Low battery alert threshold (15% or 20%)
        if (!isCharging && level <= 15 && lastLevelAlerted != level) {
            emitAlert("Low Battery $level%", durationMs = 4000L)
            lastLevelAlerted = level
        }

        lastPluggedState = isCharging
    }

    private fun emitAlert(message: String, durationMs: Long) {
        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text(message),
            centerContent = null
        )
        val event = IslandEvent.Alert(
            key = ActivityKey("battery:alert"),
            source = EventSource.BATTERY,
            timestamp = System.currentTimeMillis(),
            state = state,
            durationMs = durationMs
        )
        _events.tryEmit(event)
    }
}
```

- [ ] **Step 2: Write unit test `BatteryCollectorTest`**

`feature/battery/src/test/kotlin/io/selorax/island/feature/battery/BatteryCollectorTest.kt`:
```kotlin
package io.selorax.island.feature.battery

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BatteryCollectorTest {
    private val collector = BatteryCollector()

    @Test
    fun `plugging charger in emits charging alert`() = runBlocking {
        collector.events.test {
            collector.onBatteryStateChanged(level = 80, isCharging = true)

            val event = awaitItem() as IslandEvent.Alert
            assertThat(event.key.raw).isEqualTo("battery:alert")
            assertThat(event.durationMs).isEqualTo(3000L)
        }
    }

    @Test
    fun `low battery emits low battery alert`() = runBlocking {
        collector.events.test {
            collector.onBatteryStateChanged(level = 14, isCharging = false)

            val event = awaitItem() as IslandEvent.Alert
            assertThat(event.key.raw).isEqualTo("battery:alert")
            assertThat(event.durationMs).isEqualTo(4000L)
        }
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

Run:
```bash
./gradlew :feature:battery:testDebugUnitTest
```
Expected: PASS.

---

### Task 10: Timer Domain & Alarm Scheduler (`:feature:timer`)

**Files:**
- Create: `feature/timer/src/main/kotlin/io/selorax/island/feature/timer/TimerRepository.kt`
- Create: `feature/timer/src/main/kotlin/io/selorax/island/feature/timer/TimerCollector.kt`
- Create: `feature/timer/src/main/kotlin/io/selorax/island/feature/timer/BootReceiver.kt`
- Create: `feature/timer/src/main/AndroidManifest.xml`
- Test: `feature/timer/src/test/kotlin/io/selorax/island/feature/timer/TimerCollectorTest.kt`

**Interfaces:**
- Consumes: Timer domain model
- Produces: `EventCollector` for active countdown timers and `BootReceiver` for system reboot

- [ ] **Step 1: Implement `TimerCollector` and `BootReceiver`**

`feature/timer/src/main/kotlin/io/selorax/island/feature/timer/TimerCollector.kt`:
```kotlin
package io.selorax.island.feature.timer

import io.selorax.island.common.EventCollector
import io.selorax.island.model.ActivityKey
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.PriorityHints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class TimerCollector : EventCollector {
    override val source: EventSource = EventSource.TIMER

    private val _events = MutableSharedFlow<IslandEvent>(extraBufferCapacity = 16)
    override val events: Flow<IslandEvent> = _events.asSharedFlow()

    public fun onTimerTick(id: String, label: String, remainingSeconds: Int) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val formatted = "%02d:%02d".format(minutes, seconds)

        val state = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("timer"),
            trailingSlot = IslandSlot.Text(formatted),
            centerContent = null
        )

        val event = IslandEvent.Upsert(
            key = ActivityKey("timer:$id"),
            source = EventSource.TIMER,
            timestamp = System.currentTimeMillis(),
            state = state,
            hints = PriorityHints()
        )
        _events.tryEmit(event)
    }

    public fun onTimerFinished(id: String) {
        val event = IslandEvent.End(
            key = ActivityKey("timer:$id"),
            source = EventSource.TIMER,
            timestamp = System.currentTimeMillis(),
            reason = "timer_completed"
        )
        _events.tryEmit(event)
    }
}
```

`feature/timer/src/main/kotlin/io/selorax/island/feature/timer/BootReceiver.kt`:
```kotlin
package io.selorax.island.feature.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

public class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-arm alarms for active timers after reboot
        }
    }
}
```

`feature/timer/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <application>
        <receiver
            android:name=".BootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 2: Write unit test `TimerCollectorTest`**

`feature/timer/src/test/kotlin/io/selorax/island/feature/timer/TimerCollectorTest.kt`:
```kotlin
package io.selorax.island.feature.timer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.IslandEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TimerCollectorTest {
    private val collector = TimerCollector()

    @Test
    fun `timer tick emits upsert event`() = runBlocking {
        collector.events.test {
            collector.onTimerTick("1", "Eggs", remainingSeconds = 125)

            val event = awaitItem() as IslandEvent.Upsert
            assertThat(event.key.raw).isEqualTo("timer:1")
            assertThat(event.state.isVisible).isTrue()
        }
    }

    @Test
    fun `timer finished emits end event`() = runBlocking {
        collector.events.test {
            collector.onTimerFinished("1")

            val event = awaitItem() as IslandEvent.End
            assertThat(event.key.raw).isEqualTo("timer:1")
            assertThat(event.reason).isEqualTo("timer_completed")
        }
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

Run:
```bash
./gradlew :feature:timer:testDebugUnitTest
```
Expected: PASS.

---

### Task 11: End-to-End Build & Milestone 2 Verification

**Files:**
- Create: `docs/prototypes/week2-findings.md`

**Interfaces:**
- Consumes: All 14 modules, test suite
- Produces: Green build across all modules and documented findings for P2/P3/P0 criteria

- [ ] **Step 1: Run complete build and test suite**

Run:
```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL, all unit tests passing.

- [ ] **Step 2: Document Milestone 2 findings in `docs/prototypes/week2-findings.md`**

Record findings for:
- Normalized events pipeline across all 4 collectors.
- DataStore settings and Room v1 entities without sensitive data leaks.
- Verification evidence and test coverage.
