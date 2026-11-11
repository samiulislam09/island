package io.selorax.island.data.settings

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class IslandSettingsSerializerTest {
    private val serializer = IslandSettingsSerializer()

    @Test
    fun `serializer round trip preserves values`() = runBlocking {
        val original = IslandSettings(
            islandEnabled = true,
            anchor = "BELOW_STATUS_BAR",
            hideInLandscape = false,
            onboardingCompleted = true
        )

        val output = ByteArrayOutputStream()
        serializer.writeTo(original, output)

        val input = ByteArrayInputStream(output.toByteArray())
        val restored = serializer.readFrom(input)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `corrupted stream returns defaultValue`() = runBlocking {
        val corrupted = ByteArrayInputStream("not valid json".toByteArray())
        val restored = serializer.readFrom(corrupted)

        assertThat(restored.schemaVersion).isEqualTo(1)
        assertThat(restored.islandEnabled).isTrue()
    }
}
