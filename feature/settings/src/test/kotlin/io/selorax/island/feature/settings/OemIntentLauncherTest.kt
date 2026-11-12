package io.selorax.island.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.selorax.island.common.compatibility.OemRegistry
import io.selorax.island.common.compatibility.OemType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OemIntentLauncherTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun createLaunchIntent_forXiaomi_createsConfiguredIntent() {
        val xiaomiProfile = OemRegistry.getProfile(OemType.XIAOMI)
        assertNotNull(xiaomiProfile)

        val intent = OemIntentLauncher.createLaunchIntent(context, xiaomiProfile!!)
        assertNotNull(intent)
    }

    @Test
    fun launchSafely_fallsBackToSettingsOnActivityNotFound() {
        val xiaomiProfile = OemRegistry.getProfile(OemType.XIAOMI)
        assertNotNull(xiaomiProfile)

        // First startActivity fails with ActivityNotFoundException (custom vendor activity missing)
        // Second startActivity succeeds with fallback intent
        var attempts = 0
        every { context.startActivity(any()) } answers {
            attempts++
            if (attempts == 1) {
                throw ActivityNotFoundException("Activity not found")
            }
        }

        val result = OemIntentLauncher.launchSafely(context, xiaomiProfile!!)
        assertTrue("Launch should return true after fallback success", result)
        verify(exactly = 2) { context.startActivity(any()) }
    }

    @Test
    fun launchSafely_fallsBackToSettingsOnSecurityException() {
        val samsungProfile = OemRegistry.getProfile(OemType.SAMSUNG)
        assertNotNull(samsungProfile)

        var attempts = 0
        every { context.startActivity(any()) } answers {
            attempts++
            if (attempts == 1) {
                throw SecurityException("Permission denial")
            }
        }

        val result = OemIntentLauncher.launchSafely(context, samsungProfile!!)
        assertTrue("Launch should return true after fallback success", result)
        verify(exactly = 2) { context.startActivity(any()) }
    }
}
