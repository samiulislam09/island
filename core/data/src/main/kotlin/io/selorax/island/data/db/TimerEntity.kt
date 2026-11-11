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
