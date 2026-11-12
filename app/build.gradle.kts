plugins {
    id("island.android.application")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    implementation(project(":overlay"))
    implementation(project(":feature:notifications"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}

