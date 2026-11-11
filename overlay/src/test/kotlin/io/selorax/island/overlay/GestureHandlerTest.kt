package io.selorax.island.overlay

import com.google.common.truth.Truth.assertThat
import io.selorax.island.model.UserIntent
import org.junit.Test

class GestureHandlerTest {
    private val handler = GestureHandler()

    @Test
    fun `horizontal drag exceeding threshold returns dismiss intent`() {
        val intent = handler.onDragEnd(dragAmountX = 150f, dragAmountY = 0f, thresholdPx = 100f)
        assertThat(intent).isEqualTo(UserIntent.Dismiss())
    }

    @Test
    fun `vertical upward drag exceeding threshold returns collapse intent`() {
        val intent = handler.onDragEnd(dragAmountX = 0f, dragAmountY = -120f, thresholdPx = 100f)
        assertThat(intent).isEqualTo(UserIntent.Collapse)
    }
}
