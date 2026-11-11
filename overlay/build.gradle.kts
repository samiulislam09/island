plugins {
    id("island.android.library")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.savedstate)
}

