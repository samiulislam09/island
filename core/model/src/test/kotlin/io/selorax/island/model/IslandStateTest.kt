package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IslandStateTest {
    @Test
    fun `hidden state represents non visible overlay`() {
        val state: IslandState = IslandState.Hidden
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun `compact state contains slots`() {
        val state: IslandState = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text("85%"),
            centerContent = null
        )
        assertThat(state.isVisible).isTrue()
    }
}
