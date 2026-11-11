plugins {
    id("island.android.library")
    id("island.test")
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
}
