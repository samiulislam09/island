plugins {
    id("island.android.library")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
}
