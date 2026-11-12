package io.selorax.island.designsystem

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignTokensTest {
    @Test
    fun `island colors match design tokens`() {
        assertThat(IslandColors.Surface).isEqualTo(Color(0xFF0B0B0C))
        assertThat(IslandColors.AccentIon).isEqualTo(Color(0xFF5B8CFF))
        assertThat(IslandColors.Success).isEqualTo(Color(0xFF3DDC84))
        assertThat(IslandColors.Warning).isEqualTo(Color(0xFFFFD600))
        assertThat(IslandColors.Danger).isEqualTo(Color(0xFFFF5A5F))
        assertThat(IslandColors.OnSurfaceHigh).isEqualTo(Color(0xEBFFFFFF))
        assertThat(IslandColors.OnSurfaceMedium).isEqualTo(Color(0xA3FFFFFF))
        assertThat(IslandColors.OnSurfaceDisabled).isEqualTo(Color(0x61FFFFFF))
    }

    @Test
    fun `spring motion specs are defined`() {
        assertThat(IslandMotion.morphDamping).isEqualTo(0.82f)
        assertThat(IslandMotion.morphStiffness).isEqualTo(380f)
        assertThat(IslandMotion.contentDamping).isEqualTo(1.0f)
        assertThat(IslandMotion.contentStiffness).isEqualTo(600f)
        assertThat(IslandMotion.alertDamping).isEqualTo(0.55f)
        assertThat(IslandMotion.alertStiffness).isEqualTo(300f)
    }

    @Test
    fun `typography defines tabular figures for timer and numbers`() {
        assertThat(IslandTypography.tabularNumbers).isEqualTo("tnum")
        assertThat(IslandTypography.TimerLarge.fontFeatureSettings).isEqualTo("tnum")
        assertThat(IslandTypography.TimerMedium.fontFeatureSettings).isEqualTo("tnum")
    }
}
