plugins {
    id("island.jvm.library")
    id("island.test")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
