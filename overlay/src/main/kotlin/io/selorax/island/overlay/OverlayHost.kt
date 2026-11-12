package io.selorax.island.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.selorax.island.feature.notifications.IslandNotificationHub
import io.selorax.island.feature.notifications.IslandNotificationListener
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandAction
import io.selorax.island.model.IslandDesignFace
import io.selorax.island.model.IslandEvent
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.overlay.ui.IslandPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public class OverlayHost : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore: ViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private lateinit var windowController: WindowController
    private var composeView: ComposeView? = null

    private var lastBatteryLevel: Int = 100
    private var isCurrentlyCharging: Boolean = false

    private var defaultCompactState: IslandState = IslandState.Compact(
        leadingSlot = IslandSlot.Icon("battery"),
        trailingSlot = IslandSlot.Text("")
    )

    private val currentState = mutableStateOf<IslandState>(defaultCompactState)
    private var autoCollapseJob: Job? = null

    private val currentXOffsetPx = mutableStateOf(0)
    private val currentYOffsetPx = mutableStateOf(0)
    private val currentWidthDp = mutableStateOf(38f)
    private val currentHeightDp = mutableStateOf(28f)
    private val currentDesignFace = mutableStateOf(IslandDesignFace.AMOLED_CLASSIC)
    private val currentAccentColor = mutableStateOf(AccentGlowColor.CYAN)

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    collapse()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Resume overlay
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCurrentlyCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                    if (level >= 0 && scale > 0) {
                        lastBatteryLevel = (level * 100) / scale
                        if (currentState.value is IslandState.Compact) {
                            defaultCompactState = IslandState.Compact(
                                leadingSlot = IslandSlot.Icon(if (isCurrentlyCharging) "charging" else "battery"),
                                trailingSlot = IslandSlot.Text("")
                            )
                            currentState.value = defaultCompactState
                        }
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCurrentlyCharging = true
                    defaultCompactState = IslandState.Compact(
                        leadingSlot = IslandSlot.Icon("charging"),
                        trailingSlot = IslandSlot.Text("")
                    )
                    expandTemporarily(
                        IslandState.Expanded(
                            title = "Charging",
                            subtitle = "$lastBatteryLevel% • Super Fast Charging",
                            leadingSlot = IslandSlot.Icon("charging"),
                            contentSlot = IslandSlot.Progress(lastBatteryLevel / 100f),
                            actions = listOf(IslandAction("dismiss", "Done"))
                        ),
                        durationMs = 4500L
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCurrentlyCharging = false
                    defaultCompactState = IslandState.Compact(
                        leadingSlot = IslandSlot.Icon("battery"),
                        trailingSlot = IslandSlot.Text("")
                    )
                    expandTemporarily(
                        IslandState.Expanded(
                            title = "Unplugged",
                            subtitle = "$lastBatteryLevel% Battery remaining",
                            leadingSlot = IslandSlot.Icon("battery"),
                            contentSlot = null,
                            actions = listOf(IslandAction("dismiss", "OK"))
                        ),
                        durationMs = 2500L
                    )
                }
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            (28 * resources.displayMetrics.density).toInt()
        }
    }

    private fun updateWindowBounds(isExpanded: Boolean) {
        val density = resources.displayMetrics.density
        if (isExpanded) {
            windowController.layoutParams.width = (350 * density).toInt()
            windowController.layoutParams.height = (180 * density).toInt()
        } else {
            windowController.layoutParams.width = (currentWidthDp.value * density).toInt()
            windowController.layoutParams.height = (currentHeightDp.value * density).toInt()
        }
        windowController.updatePosition(currentXOffsetPx.value, currentYOffsetPx.value)
    }

    private var lastExpandedTimeMs: Long = 0L
    private var lastExpandedTitle: String = ""
    private var lastExpandedSubtitle: String = ""

    public fun expandTemporarily(state: IslandState.Expanded, durationMs: Long = 4500L) {
        val now = System.currentTimeMillis()
        val current = currentState.value
        // If the exact same alert is already expanded, ignore redundant re-expansion
        if (current is IslandState.Expanded &&
            current.title == state.title &&
            current.subtitle == state.subtitle &&
            now - lastExpandedTimeMs < 2000L
        ) {
            return
        }
        lastExpandedTimeMs = now
        lastExpandedTitle = state.title
        lastExpandedSubtitle = state.subtitle ?: ""

        updateWindowBounds(isExpanded = true)
        currentState.value = state
        autoCollapseJob?.cancel()
        autoCollapseJob = lifecycleScope.launch {
            delay(durationMs)
            collapse()
        }
    }

    public fun collapse() {
        autoCollapseJob?.cancel()
        currentState.value = defaultCompactState
        lifecycleScope.launch {
            delay(520L)
            if (currentState.value is IslandState.Compact) {
                updateWindowBounds(isExpanded = false)
            }
        }
    }

    public fun toggleExpand() {
        if (currentState.value is IslandState.Compact) {
            expandTemporarily(
                IslandState.Expanded(
                    title = "Dynamic Island",
                    subtitle = "Samsung Punch Hole Active",
                    leadingSlot = IslandSlot.Icon(if (isCurrentlyCharging) "charging" else "battery"),
                    contentSlot = IslandSlot.Text("$lastBatteryLevel% Battery"),
                    actions = listOf(
                        IslandAction("dismiss", "Close"),
                        IslandAction("open", "Settings")
                    )
                ),
                durationMs = 5000L
            )
        } else {
            collapse()
        }
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        val prefs = getSharedPreferences("island_position_prefs", Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density
        val defaultY = ((getStatusBarHeight() - (28 * density).toInt()) / 2)
            .coerceAtLeast((8 * density).toInt())

        val initialY = prefs.getInt("pref_y_px", defaultY)
        val initialX = prefs.getInt("pref_x_px", 0)

        // Migrate old oversized pill settings down to punch hole notch size (38dp x 28dp)
        var savedWidthDp = prefs.getFloat("pref_width_dp", 38f)
        if (savedWidthDp > 50f) {
            savedWidthDp = 38f
        }
        var savedHeightDp = prefs.getFloat("pref_height_dp", 28f)
        if (savedHeightDp > 34f) {
            savedHeightDp = 28f
        }

        currentYOffsetPx.value = initialY
        currentXOffsetPx.value = initialX
        currentWidthDp.value = savedWidthDp
        currentHeightDp.value = savedHeightDp

        val savedFaceId = prefs.getString("pref_design_face", IslandDesignFace.AMOLED_CLASSIC.id)
        val savedAccentId = prefs.getString("pref_accent_color", AccentGlowColor.CYAN.id)
        currentDesignFace.value = IslandDesignFace.fromId(savedFaceId)
        currentAccentColor.value = AccentGlowColor.fromId(savedAccentId)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowController = WindowController(windowManager)
        windowController.layoutParams.x = initialX
        windowController.layoutParams.y = initialY
        windowController.layoutParams.width = (savedWidthDp * density).toInt()
        windowController.layoutParams.height = (savedHeightDp * density).toInt()

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayHost)
            setViewTreeViewModelStoreOwner(this@OverlayHost)
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    IslandPill(
                        state = currentState.value,
                        compactWidth = currentWidthDp.value.toInt().dp,
                        compactHeight = currentHeightDp.value.toInt().dp,
                        designFace = currentDesignFace.value,
                        accentColor = currentAccentColor.value,
                        onClick = { toggleExpand() },
                        onAction = { actionId ->
                            when (actionId) {
                                "dismiss", "close" -> collapse()
                                "open" -> {
                                    collapse()
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    launchIntent?.let { startActivity(it) }
                                }
                                else -> collapse()
                            }
                        }
                    )
                }
            }
        }

        view.setOnApplyWindowInsetsListener { _, insets ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val cutout = insets.displayCutout
                if (cutout != null && !prefs.contains("pref_y_px")) {
                    val rect = cutout.boundingRectTop
                    if (rect.height() > 0) {
                        val cutoutCenterY = (rect.top + rect.bottom) / 2
                        val pillHeightPx = (currentHeightDp.value * density).toInt()
                        val calculatedY = maxOf(0, cutoutCenterY - (pillHeightPx / 2))
                        currentYOffsetPx.value = calculatedY
                        windowController.updatePosition(currentXOffsetPx.value, calculatedY)
                    }
                }
            }
            insets
        }

        composeView = view
        windowController.attachView(view)

        // Connect IslandNotificationHub direct in-process listener for all apps
        IslandNotificationHub.onNotificationReceived = { title, text, pkg ->
            expandTemporarily(
                IslandState.Expanded(
                    title = title,
                    subtitle = text,
                    leadingSlot = IslandSlot.Icon(pkg),
                    contentSlot = null,
                    actions = listOf(
                        IslandAction("dismiss", "Dismiss"),
                        IslandAction("open", "Open")
                    )
                ),
                durationMs = 4500L
            )
        }

        // Collect incoming notifications from IslandNotificationListener flow as fallback
        lifecycleScope.launch {
            IslandNotificationListener.bridge.events.collect { event ->
                if (IslandNotificationHub.onNotificationReceived != null) {
                    if (event is IslandEvent.End) {
                        collapse()
                    }
                    return@collect
                }
                when (event) {
                    is IslandEvent.Alert -> {
                        val eventState = event.state
                        if (eventState is IslandState.Expanded) {
                            expandTemporarily(eventState, event.durationMs)
                        } else if (eventState is IslandState.Compact) {
                            val alertTitle = (eventState.trailingSlot as? IslandSlot.Text)?.text ?: "Notification"
                            expandTemporarily(
                                IslandState.Expanded(
                                    title = alertTitle,
                                    subtitle = "New alert received",
                                    leadingSlot = eventState.leadingSlot,
                                    contentSlot = null,
                                    actions = listOf(
                                        IslandAction("dismiss", "Dismiss"),
                                        IslandAction("open", "Open")
                                    )
                                ),
                                durationMs = event.durationMs
                            )
                        }
                    }
                    is IslandEvent.Upsert -> {
                        val eventState = event.state
                        val notifTitle = (eventState as? IslandState.Compact)?.let {
                            (it.trailingSlot as? IslandSlot.Text)?.text
                        } ?: "Notification"
                        expandTemporarily(
                            IslandState.Expanded(
                                title = notifTitle,
                                subtitle = "New notification received",
                                leadingSlot = (eventState as? IslandState.Compact)?.leadingSlot,
                                contentSlot = null,
                                actions = listOf(
                                    IslandAction("dismiss", "Dismiss"),
                                    IslandAction("open", "Open")
                                )
                            ),
                            durationMs = 4500L
                        )
                    }
                    is IslandEvent.End -> {
                        collapse()
                    }
                    else -> {}
                }
            }
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(batteryReceiver, batteryFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_UPDATE_POSITION -> {
                val density = resources.displayMetrics.density
                if (intent.hasExtra(EXTRA_Y_DP)) {
                    val yDp = intent.getFloatExtra(EXTRA_Y_DP, 8f)
                    currentYOffsetPx.value = (yDp * density).toInt()
                }
                if (intent.hasExtra(EXTRA_X_DP)) {
                    val xDp = intent.getFloatExtra(EXTRA_X_DP, 0f)
                    currentXOffsetPx.value = (xDp * density).toInt()
                }
                if (intent.hasExtra(EXTRA_WIDTH_DP)) {
                    currentWidthDp.value = intent.getFloatExtra(EXTRA_WIDTH_DP, 38f)
                }
                if (intent.hasExtra(EXTRA_HEIGHT_DP)) {
                    currentHeightDp.value = intent.getFloatExtra(EXTRA_HEIGHT_DP, 28f)
                }
                updateWindowBounds(isExpanded = currentState.value is IslandState.Expanded)

                getSharedPreferences("island_position_prefs", Context.MODE_PRIVATE).edit()
                    .putInt("pref_y_px", currentYOffsetPx.value)
                    .putInt("pref_x_px", currentXOffsetPx.value)
                    .putFloat("pref_width_dp", currentWidthDp.value)
                    .putFloat("pref_height_dp", currentHeightDp.value)
                    .apply()
            }
            ACTION_TOGGLE_EXPAND -> {
                toggleExpand()
            }
            ACTION_SHOW_NOTIFICATION -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Notification"
                val text = intent.getStringExtra(EXTRA_TEXT) ?: "Message details"
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: "com.whatsapp"
                expandTemporarily(
                    IslandState.Expanded(
                        title = title,
                        subtitle = text,
                        leadingSlot = IslandSlot.Icon(pkg),
                        contentSlot = null,
                        actions = listOf(
                            IslandAction("dismiss", "Dismiss"),
                            IslandAction("open", "Open")
                        )
                    ),
                    durationMs = 4500L
                )
            }
            ACTION_SHOW_ALERT -> {
                val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: "charging"
                when (alertType) {
                    "charging" -> {
                        expandTemporarily(
                            IslandState.Expanded(
                                title = "Super Fast Charging",
                                subtitle = "$lastBatteryLevel% • 22 minutes until full",
                                leadingSlot = IslandSlot.Icon("charging"),
                                contentSlot = IslandSlot.Progress(lastBatteryLevel / 100f),
                                actions = listOf(IslandAction("dismiss", "Done"))
                            ),
                            durationMs = 4500L
                        )
                    }
                    "music" -> {
                        expandTemporarily(
                            IslandState.Expanded(
                                title = "Bohemian Rhapsody",
                                subtitle = "Queen • A Night at the Opera",
                                leadingSlot = IslandSlot.Icon("music"),
                                contentSlot = IslandSlot.Text("Playing • 02:45 / 05:54"),
                                actions = listOf(
                                    IslandAction("dismiss", "Close"),
                                    IslandAction("pause", "Pause")
                                )
                            ),
                            durationMs = 5500L
                        )
                    }
                    "timer" -> {
                        expandTemporarily(
                            IslandState.Expanded(
                                title = "Timer: 05:00",
                                subtitle = "Pasta Cooking Complete!",
                                leadingSlot = IslandSlot.Icon("timer"),
                                contentSlot = null,
                                actions = listOf(IslandAction("dismiss", "Stop"))
                            ),
                            durationMs = 5000L
                        )
                    }
                    "call" -> {
                        expandTemporarily(
                            IslandState.Expanded(
                                title = "Sarah Jenkins",
                                subtitle = "Incoming WhatsApp Call...",
                                leadingSlot = IslandSlot.Icon("call"),
                                contentSlot = null,
                                actions = listOf(
                                    IslandAction("dismiss", "Decline"),
                                    IslandAction("answer", "Answer")
                                )
                            ),
                            durationMs = 6000L
                        )
                    }
                    "navigation" -> {
                        expandTemporarily(
                            IslandState.Expanded(
                                title = "Turn Left in 250m",
                                subtitle = "Grand Avenue • 12 min (4.2 km)",
                                leadingSlot = IslandSlot.Icon("navigation"),
                                contentSlot = null,
                                actions = listOf(IslandAction("dismiss", "Exit"))
                            ),
                            durationMs = 5500L
                        )
                    }
                }
            }
            ACTION_UPDATE_STYLE -> {
                if (intent.hasExtra(EXTRA_DESIGN_FACE)) {
                    val faceId = intent.getStringExtra(EXTRA_DESIGN_FACE)
                    currentDesignFace.value = IslandDesignFace.fromId(faceId)
                }
                if (intent.hasExtra(EXTRA_ACCENT_COLOR)) {
                    val accentId = intent.getStringExtra(EXTRA_ACCENT_COLOR)
                    currentAccentColor.value = AccentGlowColor.fromId(accentId)
                }
                getSharedPreferences("island_position_prefs", Context.MODE_PRIVATE).edit()
                    .putString("pref_design_face", currentDesignFace.value.id)
                    .putString("pref_accent_color", currentAccentColor.value.id)
                    .apply()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        IslandNotificationHub.onNotificationReceived = null
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        autoCollapseJob?.cancel()
        windowController.detachView()
        composeView = null
        appViewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    public companion object {
        public const val ACTION_UPDATE_POSITION: String = "io.selorax.island.overlay.UPDATE_POSITION"
        public const val ACTION_TOGGLE_EXPAND: String = "io.selorax.island.overlay.TOGGLE_EXPAND"
        public const val ACTION_SHOW_NOTIFICATION: String = "io.selorax.island.overlay.SHOW_NOTIFICATION"
        public const val ACTION_SHOW_ALERT: String = "io.selorax.island.overlay.SHOW_ALERT"
        public const val ACTION_UPDATE_STYLE: String = "io.selorax.island.overlay.UPDATE_STYLE"

        public const val EXTRA_Y_DP: String = "extra_y_dp"
        public const val EXTRA_X_DP: String = "extra_x_dp"
        public const val EXTRA_WIDTH_DP: String = "extra_width_dp"
        public const val EXTRA_HEIGHT_DP: String = "extra_height_dp"

        public const val EXTRA_DESIGN_FACE: String = "extra_design_face"
        public const val EXTRA_ACCENT_COLOR: String = "extra_accent_color"

        public const val EXTRA_TITLE: String = "extra_title"
        public const val EXTRA_TEXT: String = "extra_text"
        public const val EXTRA_PACKAGE: String = "extra_package"
        public const val EXTRA_ALERT_TYPE: String = "extra_alert_type"
    }
}
