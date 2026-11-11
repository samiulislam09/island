pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "dynamic-island"

includeBuild("build-logic")

include(":app")
include(":overlay")
include(":engine")
include(":core:model")
include(":core:common")
include(":core:data")
include(":core:permissions")
include(":core:designsystem")
include(":feature:notifications")
include(":feature:media")
include(":feature:battery")
include(":feature:timer")
include(":feature:settings")
include(":feature:onboarding")