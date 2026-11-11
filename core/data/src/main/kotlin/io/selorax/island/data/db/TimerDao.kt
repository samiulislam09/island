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
