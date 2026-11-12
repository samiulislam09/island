package io.selorax.island

import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace
import io.selorax.island.ui.AppTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {

    @Test
    fun appTab_definesExpectedTabs() {
        val tabs = AppTab.entries
        assertEquals(5, tabs.size)
        assertEquals(AppTab.HOME, tabs[0])
        assertEquals(AppTab.FACES, tabs[1])
        assertEquals(AppTab.NOTCH, tabs[2])
        assertEquals(AppTab.ALERTS, tabs[3])
        assertEquals(AppTab.SETTINGS, tabs[4])
    }

    @Test
    fun appTab_titlesAndIconsAreNonEmpty() {
        AppTab.entries.forEach { tab ->
            assertTrue(tab.title.isNotBlank())
            assertTrue(tab.icon.isNotBlank())
        }
    }

    @Test
    fun islandDesignFace_containsExpectedFaces() {
        val faces = IslandDesignFace.entries
        assertEquals(6, faces.size)
        assertNotNull(IslandDesignFace.fromId("amoled"))
        assertNotNull(IslandDesignFace.fromId("cyber_neon"))
        assertNotNull(IslandDesignFace.fromId("frosted_glass"))
        assertNotNull(IslandDesignFace.fromId("aurora"))
        assertNotNull(IslandDesignFace.fromId("titanium"))
        assertNotNull(IslandDesignFace.fromId("camera_halo"))
        assertEquals(IslandDesignFace.AMOLED_CLASSIC, IslandDesignFace.fromId("unknown_face"))
    }

    @Test
    fun accentGlowColor_containsExpectedColors() {
        val accents = AccentGlowColor.entries
        assertEquals(6, accents.size)
        assertNotNull(AccentGlowColor.fromId("cyan"))
        assertNotNull(AccentGlowColor.fromId("purple"))
        assertNotNull(AccentGlowColor.fromId("green"))
        assertNotNull(AccentGlowColor.fromId("amber"))
        assertNotNull(AccentGlowColor.fromId("rose"))
        assertNotNull(AccentGlowColor.fromId("pearl"))
        assertEquals(AccentGlowColor.CYAN, AccentGlowColor.fromId("unknown_color"))
    }
}
