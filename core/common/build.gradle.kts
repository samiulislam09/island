plugins {
    id("island.jvm.library")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
