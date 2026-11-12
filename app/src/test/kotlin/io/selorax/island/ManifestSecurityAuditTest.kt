package io.selorax.island

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestSecurityAuditTest {

    private val forbiddenPermissions = listOf(
        "android.permission.INTERNET",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
    )

    private fun findProjectRoot(): File {
        var dir: File? = File(".").canonicalFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists() || File(dir, "gradlew").exists()) {
                return dir
            }
            dir = dir.parentFile
        }
        return File(".").canonicalFile
    }

    @Test
    fun appManifest_doesNotContainForbiddenPermissions() {
        val rootDir = findProjectRoot()
        val manifestFiles = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml"),
            File(rootDir, "app/src/main/AndroidManifest.xml")
        )
        val manifestFile = manifestFiles.firstOrNull { it.exists() }
        assertNotNull("App AndroidManifest.xml must exist", manifestFile)

        if (manifestFile != null) {
            val content = manifestFile.readText()
            forbiddenPermissions.forEach { permission ->
                assertFalse(
                    "App manifest (${manifestFile.path}) must not declare forbidden permission: $permission",
                    content.contains(permission)
                )
            }
        }
    }

    @Test
    fun allProjectManifests_doNotContainForbiddenPermissions() {
        val rootDir = findProjectRoot()
        val manifests = rootDir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
            .toList()

        assertTrue("Project must contain at least one AndroidManifest.xml", manifests.isNotEmpty())

        manifests.forEach { manifest ->
            val content = manifest.readText()
            forbiddenPermissions.forEach { permission ->
                assertFalse(
                    "Manifest ${manifest.path} must not declare forbidden permission: $permission",
                    content.contains(permission)
                )
            }
        }
    }
}
