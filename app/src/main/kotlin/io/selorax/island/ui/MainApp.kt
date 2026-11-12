package io.selorax.island.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.DarkDashboardColors
import io.selorax.island.designsystem.IslandTheme
import io.selorax.island.designsystem.LightDashboardColors
import io.selorax.island.designsystem.LocalDashboardColors
import io.selorax.island.designsystem.components.IslandLogoMark
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace

@Composable
public fun MainApp(
    isRunning: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    topOffset: Float,
    horizontalOffset: Float,
    width: Float,
    height: Float,
    currentFace: IslandDesignFace,
    currentAccent: AccentGlowColor,
    isLightMode: Boolean = true,
    onToggleLightMode: (Boolean) -> Unit = {},
    onToggleService: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onTopOffsetChange: (Float) -> Unit,
    onHorizontalOffsetChange: (Float) -> Unit,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onApplyPreset: (top: Float, horizontal: Float, width: Float, height: Float) -> Unit,
    onSelectFace: (IslandDesignFace) -> Unit,
    onSelectAccent: (AccentGlowColor) -> Unit,
    onTestNotification: (title: String, text: String, pkg: String) -> Unit,
    onTestAlert: (type: String) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab = remember { mutableStateOf(AppTab.HOME) }
    val colors = if (isLightMode) LightDashboardColors else DarkDashboardColors
    val accentRgb = IslandTheme.getAccent(currentAccent, isLightMode)

    CompositionLocalProvider(LocalDashboardColors provides colors) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.background,
            bottomBar = {
                NavigationBar(
                    containerColor = colors.navBackground,
                    contentColor = colors.textPrimary,
                    tonalElevation = if (isLightMode) 2.dp else 0.dp,
                    modifier = Modifier.border(
                        BorderStroke(0.8.dp, colors.navBorder),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                ) {
                    AppTab.entries.forEach { tab ->
                        val isSelected = currentTab.value == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab.value = tab },
                            icon = {
                                if (tab == AppTab.HOME) {
                                    IslandLogoMark(
                                        size = 22.dp,
                                        accentColor = if (isSelected) accentRgb else colors.textMuted,
                                        isAnimated = isSelected
                                    )
                                } else {
                                    Text(
                                        text = tab.icon,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentRgb,
                                selectedTextColor = accentRgb,
                                indicatorColor = accentRgb.copy(alpha = if (isLightMode) 0.14f else 0.18f),
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(
                    targetState = currentTab.value,
                    animationSpec = tween(durationMillis = 200),
                    label = "TabCrossfade"
                ) { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            isRunning = isRunning,
                            hasOverlayPermission = hasOverlayPermission,
                            hasNotificationPermission = hasNotificationPermission,
                            currentFace = currentFace,
                            currentAccent = currentAccent,
                            isLightMode = isLightMode,
                            onToggleLightMode = onToggleLightMode,
                            compactWidth = width,
                            compactHeight = height,
                            horizontalOffset = horizontalOffset,
                            onToggleService = onToggleService,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onOpenAppInfo = onOpenAppInfo,
                            onNavigateToTab = { currentTab.value = it },
                            onTestNotification = onTestNotification,
                            onTestAlert = onTestAlert,
                            onToggleExpand = onToggleExpand
                        )
                        AppTab.FACES -> DesignFacesScreen(
                            currentFace = currentFace,
                            currentAccent = currentAccent,
                            onSelectFace = onSelectFace,
                            onSelectAccent = onSelectAccent,
                            onTestExpandedFace = { faceType ->
                                when (faceType) {
                                    "whatsapp" -> onTestNotification(
                                        "Sarah Jenkins",
                                        "Hey! Are you free for dinner tonight around 7?",
                                        "com.whatsapp"
                                    )
                                    else -> onTestAlert(faceType)
                                }
                            },
                            onToggleExpand = onToggleExpand
                        )
                        AppTab.NOTCH -> NotchCalibrationScreen(
                            topOffset = topOffset,
                            horizontalOffset = horizontalOffset,
                            width = width,
                            height = height,
                            onTopOffsetChange = onTopOffsetChange,
                            onHorizontalOffsetChange = onHorizontalOffsetChange,
                            onWidthChange = onWidthChange,
                            onHeightChange = onHeightChange,
                            onApplyPreset = onApplyPreset,
                            onTestExpand = onToggleExpand
                        )
                        AppTab.ALERTS -> AlertsScreen(
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onOpenAppInfo = onOpenAppInfo,
                            onTestNotification = onTestNotification,
                            onTestAlert = onTestAlert
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            currentAccent = currentAccent,
                            onOpenBatteryOptimization = onOpenBatteryOptimization,
                            onOpenAppInfo = onOpenAppInfo
                        )
                    }
                }
            }
        }
    }
}
