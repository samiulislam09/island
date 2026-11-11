package io.selorax.island.data.settings

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

public class IslandSettingsSerializer(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : Serializer<IslandSettings> {
    override val defaultValue: IslandSettings = IslandSettings()

    override suspend fun readFrom(input: InputStream): IslandSettings {
        return try {
            val text = input.bufferedReader().use { it.readText() }
            if (text.isBlank()) defaultValue else json.decodeFromString<IslandSettings>(text)
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: IslandSettings, output: OutputStream) {
        val text = json.encodeToString(IslandSettings.serializer(), t)
        output.write(text.toByteArray())
    }
}
