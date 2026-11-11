plugins {
    id("island.jvm.library")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
}
