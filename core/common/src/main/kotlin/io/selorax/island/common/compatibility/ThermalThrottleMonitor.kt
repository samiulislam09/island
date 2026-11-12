package io.selorax.island.common.compatibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public enum class ThrottleLevel {
    NORMAL,
    LIGHT_THROTTLE,
    SEVERE_THROTTLE
}

public class ThermalThrottleMonitor(
    initialPowerSaveMode: Boolean = false,
    initialThermalStatus: Int = 0
) {
    private val lock: Any = Any()

    private val _throttleLevel: MutableStateFlow<ThrottleLevel> = MutableStateFlow(
        computeThrottleLevel(initialPowerSaveMode, initialThermalStatus)
    )
    public val throttleLevel: StateFlow<ThrottleLevel> = _throttleLevel.asStateFlow()

    private var currentPowerSaveMode: Boolean = initialPowerSaveMode
    private var currentThermalStatus: Int = initialThermalStatus

    public fun onPowerSaveModeChanged(isPowerSaveMode: Boolean) {
        synchronized(lock) {
            currentPowerSaveMode = isPowerSaveMode
            _throttleLevel.value = computeThrottleLevel(currentPowerSaveMode, currentThermalStatus)
        }
    }

    public fun onThermalStatusChanged(thermalStatus: Int) {
        synchronized(lock) {
            currentThermalStatus = thermalStatus
            _throttleLevel.value = computeThrottleLevel(currentPowerSaveMode, currentThermalStatus)
        }
    }

    public companion object {
        // Platform thermal status constants (API 29+)
        public const val STATUS_NONE: Int = 0
        public const val STATUS_LIGHT: Int = 1
        public const val STATUS_MODERATE: Int = 2
        public const val STATUS_SEVERE: Int = 3
        public const val STATUS_CRITICAL: Int = 4
        public const val STATUS_EMERGENCY: Int = 5

        public fun computeThrottleLevel(isPowerSaveMode: Boolean, thermalStatus: Int): ThrottleLevel {
            return when {
                thermalStatus >= STATUS_SEVERE -> ThrottleLevel.SEVERE_THROTTLE
                isPowerSaveMode || thermalStatus == STATUS_MODERATE -> ThrottleLevel.LIGHT_THROTTLE
                else -> ThrottleLevel.NORMAL
            }
        }
    }
}
