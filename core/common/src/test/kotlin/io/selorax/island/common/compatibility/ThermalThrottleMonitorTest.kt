package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ThermalThrottleMonitorTest {

    @Test
    fun computeThrottleLevel_normalConditions_returnsNormal() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_NONE
        )
        assertThat(level).isEqualTo(ThrottleLevel.NORMAL)
    }

    @Test
    fun computeThrottleLevel_thermalLight_returnsNormal() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_LIGHT
        )
        assertThat(level).isEqualTo(ThrottleLevel.NORMAL)
    }

    @Test
    fun computeThrottleLevel_powerSaveOn_returnsLightThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = true,
            thermalStatus = ThermalThrottleMonitor.STATUS_NONE
        )
        assertThat(level).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_thermalModerate_returnsLightThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_MODERATE
        )
        assertThat(level).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_thermalSevere_returnsSevereThrottle() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_SEVERE
        )
        assertThat(level).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_thermalCriticalOrEmergency_returnsSevereThrottle() {
        val criticalLevel = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_CRITICAL
        )
        assertThat(criticalLevel).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)

        val emergencyLevel = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = false,
            thermalStatus = ThermalThrottleMonitor.STATUS_EMERGENCY
        )
        assertThat(emergencyLevel).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)
    }

    @Test
    fun computeThrottleLevel_severeTakesPrecedenceOverPowerSave() {
        val level = ThermalThrottleMonitor.computeThrottleLevel(
            isPowerSaveMode = true,
            thermalStatus = ThermalThrottleMonitor.STATUS_SEVERE
        )
        assertThat(level).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)
    }

    @Test
    fun instance_initialStateMatchesConstructor() {
        val monitor = ThermalThrottleMonitor(
            initialPowerSaveMode = true,
            initialThermalStatus = ThermalThrottleMonitor.STATUS_NONE
        )
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)
    }

    @Test
    fun instance_onPowerSaveModeChanged_updatesFlow() {
        val monitor = ThermalThrottleMonitor()
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.NORMAL)

        monitor.onPowerSaveModeChanged(true)
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)

        monitor.onPowerSaveModeChanged(false)
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.NORMAL)
    }

    @Test
    fun instance_onThermalStatusChanged_updatesFlow() {
        val monitor = ThermalThrottleMonitor()
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.NORMAL)

        monitor.onThermalStatusChanged(ThermalThrottleMonitor.STATUS_MODERATE)
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.LIGHT_THROTTLE)

        monitor.onThermalStatusChanged(ThermalThrottleMonitor.STATUS_SEVERE)
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.SEVERE_THROTTLE)

        monitor.onThermalStatusChanged(ThermalThrottleMonitor.STATUS_NONE)
        assertThat(monitor.throttleLevel.value).isEqualTo(ThrottleLevel.NORMAL)
    }
}
