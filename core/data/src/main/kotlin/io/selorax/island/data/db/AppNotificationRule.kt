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
