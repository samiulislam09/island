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
