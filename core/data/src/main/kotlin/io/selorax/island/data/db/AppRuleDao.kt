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
