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
    private val _permissions: MutableStateFlow<PermissionSnapshot> = MutableStateFlow(checkPermissions())
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
