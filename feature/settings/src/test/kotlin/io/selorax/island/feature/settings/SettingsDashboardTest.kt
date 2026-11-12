package io.selorax.island.feature.settings

import io.selorax.island.common.compatibility.CutoutType
import io.selorax.island.common.compatibility.DeviceCapabilities
import io.selorax.island.common.compatibility.OemRegistry
import io.selorax.island.common.compatibility.OemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDashboardTest {

    @Test
    fun oemProfile_availableForXiaomi_omittedForStock() {
        val xiaomiCaps = DeviceCapabilities(
            oemType = OemType.XIAOMI,
            cutoutType = CutoutType.CENTER_PUNCH,
            isFoldable = false,
            oemProfile = OemRegistry.getProfile(OemType.XIAOMI)
        )
        assertNotNull(xiaomiCaps.oemProfile)

        val stockCaps = DeviceCapabilities(
            oemType = OemType.STOCK_OR_OTHER,
            cutoutType = CutoutType.CENTER_PUNCH,
            isFoldable = false,
            oemProfile = OemRegistry.getProfile(OemType.STOCK_OR_OTHER)
        )
        assertNull(stockCaps.oemProfile)
    }

    @Test
    fun oemProfiles_availableForSupportedVendors() {
        val vendors = listOf(
            OemType.XIAOMI,
            OemType.SAMSUNG,
            OemType.OPPO_ONEPLUS,
            OemType.VIVO
        )
        vendors.forEach { vendor ->
            val profile = OemRegistry.getProfile(vendor)
            assertNotNull("Profile should exist for $vendor", profile)
            assertEquals(vendor, profile?.oemType)
            assertTrue("Instructions should not be empty for $vendor", profile?.instructions?.isNotEmpty() == true)
        }
    }

    @Test
    fun oemProfile_xiaomiSecurityCenterComponentConfigured() {
        val xiaomi = OemRegistry.getProfile(OemType.XIAOMI)
        assertNotNull(xiaomi)
        assertEquals("com.miui.securitycenter", xiaomi?.settingsPackage)
        assertEquals("com.miui.permcenter.permissions.PermissionsEditorActivity", xiaomi?.settingsComponent)
    }
}
