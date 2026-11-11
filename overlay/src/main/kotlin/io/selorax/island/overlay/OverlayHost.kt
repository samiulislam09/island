package io.selorax.island.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.overlay.ui.IslandPill

public class OverlayHost : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private val savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore: ViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private lateinit var windowController: WindowController
    private var composeView: ComposeView? = null
    private val currentState = mutableStateOf<IslandState>(
        IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text("100%")
        )
    )

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Suspend overlay rendering during screen off
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Resume overlay rendering
                }
            }
        }
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowController = WindowController(windowManager)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayHost)
            setViewTreeViewModelStoreOwner(this@OverlayHost)
            setContent {
                IslandPill(
                    state = currentState.value,
                    onClick = {
                        currentState.value = if (currentState.value is IslandState.Compact) {
                            IslandState.Expanded(
                                title = "Island Test",
                                subtitle = "Compact -> Expanded Transition",
                                leadingSlot = IslandSlot.Icon("test"),
                                contentSlot = IslandSlot.Text("Details"),
                                actions = emptyList()
                            )
                        } else {
                            IslandState.Compact(
                                leadingSlot = IslandSlot.Icon("battery"),
                                trailingSlot = IslandSlot.Text("100%")
                            )
                        }
                    }
                )
            }
        }

        composeView = view
        windowController.attachView(view)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        windowController.detachView()
        composeView = null
        appViewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
