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
