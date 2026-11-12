package io.selorax.island.common.compatibility

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class OemRegistryTest {

    @Test
    fun getProfile_xiaomi_returnsAutostartAndPopupsProfile() {
        val profile = OemRegistry.getProfile(OemType.XIAOMI)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("Xiaomi")
        assertThat(profile?.settingsPackage).isEqualTo("com.miui.securitycenter")
    }

    @Test
    fun getProfile_samsung_returnsBatteryOptimizationProfile() {
        val profile = OemRegistry.getProfile(OemType.SAMSUNG)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("Samsung")
    }

    @Test
    fun getProfile_oppo_returnsStartupManagerProfile() {
        val profile = OemRegistry.getProfile(OemType.OPPO_ONEPLUS)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("OnePlus")
    }

    @Test
    fun getProfile_vivo_returnsHighBackgroundPowerUsageProfile() {
        val profile = OemRegistry.getProfile(OemType.VIVO)
        assertThat(profile).isNotNull()
        assertThat(profile?.title).contains("Vivo")
        assertThat(profile?.settingsPackage).isEqualTo("com.vivo.permissionmanager")
    }

    @Test
    fun getProfile_stock_returnsNull() {
        val profile = OemRegistry.getProfile(OemType.STOCK_OR_OTHER)
        assertThat(profile).isNull()
    }
}
