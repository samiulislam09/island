plugins {
    id("island.android.library")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
}
