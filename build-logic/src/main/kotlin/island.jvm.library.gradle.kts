plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

tasks.withType<Test> {
    useJUnitPlatform()
}
