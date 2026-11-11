package io.selorax.island.data.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

public class SettingsRepository(
    private val dataStore: DataStore<IslandSettings>
) {
    public val settings: Flow<IslandSettings> = dataStore.data

    public suspend fun updateIslandEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(islandEnabled = enabled) }
    }

    public suspend fun updateAnchor(anchor: String) {
        dataStore.updateData { it.copy(anchor = anchor) }
    }

    public suspend fun completeOnboarding() {
        dataStore.updateData { it.copy(onboardingCompleted = true) }
    }

    public suspend fun acceptNotificationDisclosure() {
        dataStore.updateData { it.copy(notificationDisclosureAccepted = true) }
    }
}
