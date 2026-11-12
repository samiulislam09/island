package io.selorax.island.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.selorax.island.common.compatibility.DeviceCapabilities
import io.selorax.island.common.compatibility.OemRestrictionProfile
import io.selorax.island.data.settings.IslandSettings
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.SettingsSection
import io.selorax.island.designsystem.components.SettingsSliderRow
import io.selorax.island.designsystem.components.SettingsSwitchRow

@Composable
public fun SettingsDashboard(
    settings: IslandSettings,
    onUpdateEnabled: (Boolean) -> Unit,
    onUpdateWidth: (Float) -> Unit,
    onUpdateHeight: (Float) -> Unit,
    onUpdateHideInLandscape: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deviceCapabilities: DeviceCapabilities? = null,
    onOpenOemSettings: ((OemRestrictionProfile) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val handleOpenSettings: (OemRestrictionProfile) -> Unit = onOpenOemSettings ?: { profile ->
        OemIntentLauncher.launchSafely(context, profile)
        Unit
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IslandColors.Surface)
            .padding(vertical = 16.dp)
    ) {
        val oemProfile = deviceCapabilities?.oemProfile
        if (oemProfile != null) {
            item {
                OemGuidanceCard(
                    profile = oemProfile,
                    onOpenSettings = handleOpenSettings
                )
            }
        }

        item {
            SettingsSection(title = "General") {
                SettingsSwitchRow(
                    title = "Enable Island",
                    subtitle = "Show floating overlay around camera cutout",
                    checked = settings.islandEnabled,
                    onCheckedChange = onUpdateEnabled
                )
                SettingsSwitchRow(
                    title = "Hide in Landscape",
                    subtitle = "Suppress overlay during fullscreen video and games",
                    checked = settings.hideInLandscape,
                    onCheckedChange = onUpdateHideInLandscape
                )
            }
        }

        item {
            SettingsSection(title = "Geometry & Sizing") {
                SettingsSliderRow(
                    title = "Compact Width",
                    value = settings.compactWidthDp,
                    range = 100f..240f,
                    displayValue = "${settings.compactWidthDp.toInt()} dp",
                    onValueChange = onUpdateWidth
                )
                SettingsSliderRow(
                    title = "Compact Height",
                    value = settings.compactHeightDp,
                    range = 28f..48f,
                    displayValue = "${settings.compactHeightDp.toInt()} dp",
                    onValueChange = onUpdateHeight
                )
            }
        }
    }
}
