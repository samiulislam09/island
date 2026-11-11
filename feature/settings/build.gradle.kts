plugins {
    id("island.android.library")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
}
