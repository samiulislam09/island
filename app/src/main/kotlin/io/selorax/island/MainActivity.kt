package io.selorax.island

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace
import io.selorax.island.overlay.OverlayHost
import io.selorax.island.ui.MainApp

public class MainActivity : ComponentActivity() {
    private val hasOverlayPermission = mutableStateOf(false)
    private val hasNotificationPermission = mutableStateOf(false)
    private val isOverlayRunning = mutableStateOf(false)

    private val topOffsetDp = mutableFloatStateOf(8f)
    private val horizontalOffsetDp = mutableFloatStateOf(0f)
    private val compactWidthDp = mutableFloatStateOf(38f)
    private val compactHeightDp = mutableFloatStateOf(28f)

    private val currentDesignFace = mutableStateOf(IslandDesignFace.AMOLED_CLASSIC)
    private val currentAccentColor = mutableStateOf(AccentGlowColor.CYAN)
    private val isLightMode = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreferences()
        updatePermissionState()

        setContent {
            MaterialTheme {
                MainApp(
                    isRunning = isOverlayRunning.value,
                    hasOverlayPermission = hasOverlayPermission.value,
                    hasNotificationPermission = hasNotificationPermission.value,
                    isLightMode = isLightMode.value,
                    onToggleLightMode = {
                        isLightMode.value = it
                        savePreferences()
                    },
                    topOffset = topOffsetDp.floatValue,
                    horizontalOffset = horizontalOffsetDp.floatValue,
                    width = compactWidthDp.floatValue,
                    height = compactHeightDp.floatValue,
                    currentFace = currentDesignFace.value,
                    currentAccent = currentAccentColor.value,
                    onToggleService = { start ->
                        if (start) {
                            if (!Settings.canDrawOverlays(this)) {
                                requestOverlayPermission()
                            } else {
                                startOverlayService()
                                isOverlayRunning.value = true
                                applyCurrentPosition()
                                applyCurrentStyle()
                            }
                        } else {
                            stopOverlayService()
                            isOverlayRunning.value = false
                        }
                    },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onOpenAppInfo = { openAppInfo() },
                    onOpenBatteryOptimization = { openBatteryOptimization() },
                    onTopOffsetChange = {
                        topOffsetDp.floatValue = it
                        savePreferences()
                        applyCurrentPosition()
                    },
                    onHorizontalOffsetChange = {
                        horizontalOffsetDp.floatValue = it
                        savePreferences()
                        applyCurrentPosition()
                    },
                    onWidthChange = {
                        compactWidthDp.floatValue = it
                        savePreferences()
                        applyCurrentPosition()
                    },
                    onHeightChange = {
                        compactHeightDp.floatValue = it
                        savePreferences()
                        applyCurrentPosition()
                    },
                    onApplyPreset = { top, horizontal, w, h ->
                        topOffsetDp.floatValue = top
                        horizontalOffsetDp.floatValue = horizontal
                        compactWidthDp.floatValue = w
                        compactHeightDp.floatValue = h
                        savePreferences()
                        applyCurrentPosition()
                    },
                    onSelectFace = { face ->
                        currentDesignFace.value = face
                        savePreferences()
                        applyCurrentStyle()
                    },
                    onSelectAccent = { accent ->
                        currentAccentColor.value = accent
                        savePreferences()
                        applyCurrentStyle()
                    },
                    onTestNotification = { title, text, pkg ->
                        testNotification(title = title, text = text, pkg = pkg)
                    },
                    onTestAlert = { alertType ->
                        testAlert(alertType)
                    },
                    onToggleExpand = {
                        testToggleExpand()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("island_position_prefs", Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density

        var savedWidthDp = prefs.getFloat("pref_width_dp", 38f)
        if (savedWidthDp > 80f || savedWidthDp < 24f) {
            savedWidthDp = 38f
        }
        var savedHeightDp = prefs.getFloat("pref_height_dp", 28f)
        if (savedHeightDp > 36f || savedHeightDp < 20f) {
            savedHeightDp = 28f
        }

        val savedYPx = prefs.getInt("pref_y_px", (8 * density).toInt())
        val savedXPx = prefs.getInt("pref_x_px", 0)

        topOffsetDp.floatValue = (savedYPx / density)
        horizontalOffsetDp.floatValue = (savedXPx / density)
        compactWidthDp.floatValue = savedWidthDp
        compactHeightDp.floatValue = savedHeightDp

        val savedFace = prefs.getString("pref_design_face", IslandDesignFace.AMOLED_CLASSIC.id)
        val savedAccent = prefs.getString("pref_accent_color", AccentGlowColor.CYAN.id)
        currentDesignFace.value = IslandDesignFace.fromId(savedFace)
        currentAccentColor.value = AccentGlowColor.fromId(savedAccent)
        isLightMode.value = prefs.getBoolean("pref_light_mode", true)
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences("island_position_prefs", Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density
        prefs.edit()
            .putFloat("pref_width_dp", compactWidthDp.floatValue)
            .putFloat("pref_height_dp", compactHeightDp.floatValue)
            .putInt("pref_y_px", (topOffsetDp.floatValue * density).toInt())
            .putInt("pref_x_px", (horizontalOffsetDp.floatValue * density).toInt())
            .putString("pref_design_face", currentDesignFace.value.id)
            .putString("pref_accent_color", currentAccentColor.value.id)
            .putBoolean("pref_light_mode", isLightMode.value)
            .apply()
    }

    private fun applyCurrentPosition() {
        val intent = Intent(this, OverlayHost::class.java).apply {
            action = OverlayHost.ACTION_UPDATE_POSITION
            putExtra(OverlayHost.EXTRA_Y_DP, topOffsetDp.floatValue)
            putExtra(OverlayHost.EXTRA_X_DP, horizontalOffsetDp.floatValue)
            putExtra(OverlayHost.EXTRA_WIDTH_DP, compactWidthDp.floatValue)
            putExtra(OverlayHost.EXTRA_HEIGHT_DP, compactHeightDp.floatValue)
        }
        startService(intent)
    }

    private fun applyCurrentStyle() {
        val intent = Intent(this, OverlayHost::class.java).apply {
            action = OverlayHost.ACTION_UPDATE_STYLE
            putExtra(OverlayHost.EXTRA_DESIGN_FACE, currentDesignFace.value.id)
            putExtra(OverlayHost.EXTRA_ACCENT_COLOR, currentAccentColor.value.id)
        }
        startService(intent)
    }

    private fun testToggleExpand() {
        val intent = Intent(this, OverlayHost::class.java).apply {
            action = OverlayHost.ACTION_TOGGLE_EXPAND
        }
        startService(intent)
    }

    private fun testNotification(title: String, text: String, pkg: String) {
        val intent = Intent(this, OverlayHost::class.java).apply {
            action = OverlayHost.ACTION_SHOW_NOTIFICATION
            putExtra(OverlayHost.EXTRA_TITLE, title)
            putExtra(OverlayHost.EXTRA_TEXT, text)
            putExtra(OverlayHost.EXTRA_PACKAGE, pkg)
        }
        startService(intent)
    }

    private fun testAlert(alertType: String) {
        val intent = Intent(this, OverlayHost::class.java).apply {
            action = OverlayHost.ACTION_SHOW_ALERT
            putExtra(OverlayHost.EXTRA_ALERT_TYPE, alertType)
        }
        startService(intent)
    }

    private fun updatePermissionState() {
        hasOverlayPermission.value = Settings.canDrawOverlays(this)

        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        var enabled = false
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == packageName) {
                    enabled = true
                    break
                }
            }
        }
        hasNotificationPermission.value = enabled
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun openAppInfo() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openBatteryOptimization() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openAppInfo()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        startService(intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        stopService(intent)
    }
}
