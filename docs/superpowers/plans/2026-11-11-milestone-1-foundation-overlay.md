# Milestone 1 (Foundation & Floating Overlay) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the multi-module Android build system with Gradle version catalog and composite convention plugins, scaffold all 14 project modules, implement core JVM domain models, and build the cutout-aware floating overlay window system in Jetpack Compose.

**Architecture:** A composite Gradle build (`build-logic`) enforces build conventions across 14 modules. Pure domain models live in `:core:model` and `:core:common` with zero Android framework dependencies. `:overlay` hosts a Compose view tree in system `WindowManager` via `OverlayHost` (`TYPE_APPLICATION_OVERLAY`), using `WindowController` to throttle window updates to ≤ 2 per transition and `PositionCalculator` to geometrically wrap device camera cutouts.

**Tech Stack:** Kotlin 2.1.10, Android Gradle Plugin 8.8.2, Jetpack Compose BOM 2025.02.00, Coroutines 1.10.1, JUnit 5, MockK, Truth. Target SDK: 36, Compile SDK: 37, Min SDK: 29, JVM: 21.

**Spec:** [`docs/superpowers/specs/2026-11-11-milestone-1-foundation-overlay-design.md`](file:///Volumes/work/personal/dynamic-island/docs/superpowers/specs/2026-11-11-milestone-1-foundation-overlay-design.md)

## Global Constraints

- Android API: `minSdk = 29`, `targetSdk = 36`, `compileSdk = 37`, JVM toolchain = 21.
- No `android.*` framework dependencies permitted in `:core:model` or `:core:common` (`explicitApi()` enabled).
- No network access or `INTERNET` permission in any manifest.
- Window resizing via `updateViewLayout` must not exceed 2 calls per state transition (ADR-03).
- Overlay must automatically collapse and hide (`IslandState.Hidden`) in landscape orientation.

## Review Focus

1. Device with zero display cutout: `PositionCalculator` must position the pill top-center with status bar clearance rather than crash or place at (0, 0).
2. Permission revocation mid-run: `OverlayHost` must handle `BadTokenException` or `SecurityException` gracefully during window add/update.
3. Rapid state transitions: `WindowController` must coalesce layout resize requests without queuing infinite IPC updates.
4. Screen-off power drain: `OverlayHost` must pause Compose recomposition when `ACTION_SCREEN_OFF` is received.
5. Touch pass-through: Overlay layout flags must ensure touches 1 dp outside the pill reach the underlying app without interception.

---

### Task 1: Git Repository Initialization & Template Preservation

**Files:**
- Create: `.gitignore` (update if needed)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: Existing files in `/Volumes/work/personal/dynamic-island`
- Produces: Clean Git repository with template commit `chore: initial IntelliJ template`

- [ ] **Step 1: Check existing git status and initialize repository**

Run:
```bash
git init
git add -A
git commit -m "chore: initial IntelliJ template"
```

- [ ] **Step 2: Verify git log has the template commit**

Run:
```bash
git log -n 1 --oneline
```
Expected: Output showing `chore: initial IntelliJ template`.

---

### Task 2: Gradle Version Catalog & Root Build Script Configuration

**Files:**
- Create: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `gradle.properties`

**Interfaces:**
- Consumes: Gradle 9.6 wrapper, JDK 21 toolchain
- Produces: Pinned version catalog `libs` accessible across composite builds

- [ ] **Step 1: Create version catalog `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.8.2"
kotlin = "2.1.10"
compose-bom = "2025.02.00"
compose-compiler = "2.1.10"
core-ktx = "1.15.0"
lifecycle = "2.8.7"
coroutines = "1.10.1"
serialization = "1.8.0"
junit = "4.13.2"
junit-jupiter = "5.11.4"
truth = "1.4.4"
mockk = "1.13.16"
turbine = "1.2.0"

[libraries]
android-gradle-plugin = { module = "com.android.tools.build:gradle", version.ref = "agp" }
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
compose-compiler-gradle-plugin = { module = "org.jetbrains.kotlin:compose-compiler-gradle-plugin", version.ref = "kotlin" }

androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-service = { module = "androidx.lifecycle:lifecycle-service", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-material3 = { module = "androidx.compose.material3:material3" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

junit = { module = "junit:junit", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "compose-compiler" }
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Update root `build.gradle.kts` and `settings.gradle.kts`**

Replace root `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
}
```

Update `settings.gradle.kts`:
```kotlin
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
```

- [ ] **Step 3: Update `gradle.properties`**

Append to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 4: Verify Gradle syncs clean with the catalog**

Run:
```bash
./gradlew help
```
Expected: BUILD SUCCESSFUL.

---

### Task 3: Composite Build `build-logic` Convention Plugins

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/island.jvm.library.gradle.kts`
- Create: `build-logic/src/main/kotlin/island.android.library.gradle.kts`
- Create: `build-logic/src/main/kotlin/island.android.application.gradle.kts`
- Create: `build-logic/src/main/kotlin/island.android.compose.gradle.kts`
- Create: `build-logic/src/main/kotlin/island.test.gradle.kts`
- Modify: `settings.gradle.kts` (add `includeBuild("build-logic")`)

**Interfaces:**
- Consumes: `libs.versions.toml`
- Produces: Reusable Gradle convention plugins `island.jvm.library`, `island.android.library`, `island.android.application`, `island.android.compose`, `island.test`

- [ ] **Step 1: Create `build-logic/settings.gradle.kts` and `build-logic/build.gradle.kts`**

`build-logic/settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

`build-logic/build.gradle.kts`:
```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}
```

- [ ] **Step 2: Create precompiled convention plugins in `build-logic/src/main/kotlin/`**

`island.jvm.library.gradle.kts`:
```kotlin
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
```

`island.android.library.gradle.kts`:
```kotlin
import com.android.build.gradle.LibraryExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
```

`island.android.application.gradle.kts`:
```kotlin
import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

extensions.configure<ApplicationExtension> {
    compileSdk = 37

    defaultConfig {
        applicationId = "io.selorax.island"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
```

`island.android.compose.gradle.kts`:
```kotlin
import com.android.build.api.dsl.CommonExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<CommonExtension<*, *, *, *, *, *>> {
    buildFeatures {
        compose = true
    }
}

dependencies {
    val bom = libs.findLibrary("compose-bom").get()
    add("implementation", platform(bom))
    add("implementation", libs.findLibrary("compose-ui").get())
    add("implementation", libs.findLibrary("compose-foundation").get())
    add("implementation", libs.findLibrary("compose-material3").get())
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
}
```

`island.test.gradle.kts`:
```kotlin
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", libs.findLibrary("junit").get())
    add("testImplementation", libs.findLibrary("truth").get())
    add("testImplementation", libs.findLibrary("mockk").get())
    add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
    add("testImplementation", libs.findLibrary("turbine").get())
}
```

- [ ] **Step 3: Register `includeBuild("build-logic")` in `settings.gradle.kts`**

Add to `settings.gradle.kts`:
```kotlin
includeBuild("build-logic")
```

- [ ] **Step 4: Verify build-logic compiles**

Run:
```bash
./gradlew --dry-run
```
Expected: BUILD SUCCESSFUL.

---

### Task 4: Module Skeletons Scaffolding (14 Modules)

**Files:**
- Modify: `settings.gradle.kts`
- Delete: `src/main/kotlin/Main.kt`
- Create: `app/build.gradle.kts`
- Create: `core/model/build.gradle.kts`
- Create: `core/common/build.gradle.kts`
- Create: `core/data/build.gradle.kts`
- Create: `core/permissions/build.gradle.kts`
- Create: `core/designsystem/build.gradle.kts`
- Create: `engine/build.gradle.kts`
- Create: `overlay/build.gradle.kts`
- Create: `feature/notifications/build.gradle.kts`
- Create: `feature/media/build.gradle.kts`
- Create: `feature/battery/build.gradle.kts`
- Create: `feature/timer/build.gradle.kts`
- Create: `feature/settings/build.gradle.kts`
- Create: `feature/onboarding/build.gradle.kts`

**Interfaces:**
- Consumes: Convention plugins
- Produces: Project graph with 14 modular build targets

- [ ] **Step 1: Delete template `src/main/kotlin/Main.kt`**

Run:
```bash
rm -rf src/main/kotlin/Main.kt
```

- [ ] **Step 2: Add module includes to `settings.gradle.kts`**

Append to `settings.gradle.kts`:
```kotlin
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
```

- [ ] **Step 3: Create module `build.gradle.kts` files**

`:core:model/build.gradle.kts`:
```kotlin
plugins {
    id("island.jvm.library")
    id("island.test")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

`:core:common/build.gradle.kts`:
```kotlin
plugins {
    id("island.jvm.library")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
```

`:overlay/build.gradle.kts`:
```kotlin
plugins {
    id("island.android.library")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
```

`:app/build.gradle.kts`:
```kotlin
plugins {
    id("island.android.application")
    id("island.android.compose")
    id("island.test")
}

dependencies {
    implementation(project(":overlay"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
```

Create stub `build.gradle.kts` for remaining modules (`:core:data`, `:core:permissions`, `:core:designsystem`, `:engine`, `:feature:notifications`, `:feature:media`, `:feature:battery`, `:feature:timer`, `:feature:settings`, `:feature:onboarding`):
- Pure JVM stubs (`:engine`): apply `island.jvm.library`.
- Android library stubs: apply `island.android.library`.

- [ ] **Step 4: Verify all modules are recognized by Gradle**

Run:
```bash
./gradlew projects
```
Expected: All 14 modules listed in project hierarchy.

---

### Task 5: Domain Models Implementation (`:core:model` & `:core:common`)

**Files:**
- Create: `core/model/src/main/kotlin/io/selorax/island/model/IslandState.kt`
- Create: `core/model/src/main/kotlin/io/selorax/island/model/LiveActivity.kt`
- Create: `core/model/src/main/kotlin/io/selorax/island/model/UserIntent.kt`
- Create: `core/common/src/main/kotlin/io/selorax/island/common/DispatcherProvider.kt`
- Create: `core/common/src/main/kotlin/io/selorax/island/common/IslandResult.kt`
- Test: `core/model/src/test/kotlin/io/selorax/island/model/IslandStateTest.kt`

**Interfaces:**
- Consumes: Nothing
- Produces: `IslandState`, `LiveActivity`, `UserIntent`, `DispatcherProvider`, `IslandResult`

- [ ] **Step 1: Write unit test `IslandStateTest`**

`core/model/src/test/kotlin/io/selorax/island/model/IslandStateTest.kt`:
```kotlin
package io.selorax.island.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IslandStateTest {
    @Test
    fun `hidden state represents non visible overlay`() {
        val state: IslandState = IslandState.Hidden
        assertThat(state.isVisible).isFalse()
    }

    @Test
    fun `compact state contains slots`() {
        val state: IslandState = IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text("85%"),
            centerContent = null
        )
        assertThat(state.isVisible).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:model:test
```
Expected: Compilation failure (unresolved reference: `IslandState`).

- [ ] **Step 3: Implement domain models in `core/model`**

`core/model/src/main/kotlin/io/selorax/island/model/IslandState.kt`:
```kotlin
package io.selorax.island.model

public sealed interface IslandSlot {
    public data class Icon(public val iconName: String) : IslandSlot
    public data class Text(public val text: String) : IslandSlot
    public data class Progress(public val progress: Float) : IslandSlot
}

public sealed interface IslandState {
    public val isVisible: Boolean

    public data object Hidden : IslandState {
        override val isVisible: Boolean = false
    }

    public data class Compact(
        public val leadingSlot: IslandSlot,
        public val trailingSlot: IslandSlot,
        public val centerContent: IslandSlot? = null
    ) : IslandState {
        override val isVisible: Boolean = true
    }

    public data class Expanded(
        public val title: String,
        public val subtitle: String?,
        public val leadingSlot: IslandSlot?,
        public val contentSlot: IslandSlot?,
        public val actions: List<IslandAction> = emptyList()
    ) : IslandState {
        override val isVisible: Boolean = true
    }

    public data class Split(
        public val primary: IslandSlot,
        public val secondary: IslandSlot
    ) : IslandState {
        override val isVisible: Boolean = true
    }
}

public data class IslandAction(
    public val id: String,
    public val label: String,
    public val iconName: String? = null
)
```

`core/model/src/main/kotlin/io/selorax/island/model/LiveActivity.kt`:
```kotlin
package io.selorax.island.model

public enum class ActivitySource {
    NOTIFICATION,
    MEDIA,
    BATTERY,
    TIMER
}

public enum class ActivityPriority {
    CRITICAL,
    HIGH,
    DEFAULT,
    LOW
}

public data class LiveActivity(
    public val id: String,
    public val source: ActivitySource,
    public val priority: ActivityPriority,
    public val timestamp: Long,
    public val timeoutMs: Long? = null,
    public val state: IslandState
)
```

`core/model/src/main/kotlin/io/selorax/island/model/UserIntent.kt`:
```kotlin
package io.selorax.island.model

public enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

public sealed interface UserIntent {
    public data class Tap(public val activityId: String) : UserIntent
    public data class LongPress(public val activityId: String) : UserIntent
    public data class Swipe(public val direction: Direction) : UserIntent
    public data class ActionClick(public val activityId: String, public val actionId: String) : UserIntent
}
```

`core/common/src/main/kotlin/io/selorax/island/common/DispatcherProvider.kt`:
```kotlin
package io.selorax.island.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public interface DispatcherProvider {
    public val main: CoroutineDispatcher
    public val io: CoroutineDispatcher
    public val default: CoroutineDispatcher
    public val unconfined: CoroutineDispatcher
}

public class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
```

`core/common/src/main/kotlin/io/selorax/island/common/IslandResult.kt`:
```kotlin
package io.selorax.island.common

public sealed interface IslandResult<out T> {
    public data class Success<out T>(public val data: T) : IslandResult<T>
    public data class Failure(public val throwable: Throwable) : IslandResult<Nothing>
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :core:model:test :core:common:test
```
Expected: BUILD SUCCESSFUL, tests passing.

---

### Task 6: Position Calculator (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/PositionCalculator.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/PositionCalculatorTest.kt`

**Interfaces:**
- Consumes: `CutoutSpecs` (geometry abstraction for testability without Android `DisplayCutout` runtime)
- Produces: `PillBounds` (x, y, width, height, isVisible)

- [ ] **Step 1: Write unit test `PositionCalculatorTest`**

`overlay/src/test/kotlin/io/selorax/island/overlay/PositionCalculatorTest.kt`:
```kotlin
package io.selorax.island.overlay

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PositionCalculatorTest {
    private val calculator = PositionCalculator()

    @Test
    fun `center punch hole computes centered pill coordinates`() {
        val input = CutoutSpecs(
            displayWidth = 1080,
            displayHeight = 2400,
            cutoutBoundingRect = Rect(left = 490, top = 20, right = 590, bottom = 120),
            statusBarHeight = 130,
            density = 2.75f,
            isLandscape = false
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isTrue()
        // Pill centered horizontally at displayWidth / 2 = 540
        assertThat(bounds.centerX).isEqualTo(540)
    }

    @Test
    fun `no cutout fallback places pill top center below status bar`() {
        val input = CutoutSpecs(
            displayWidth = 1080,
            displayHeight = 2400,
            cutoutBoundingRect = null,
            statusBarHeight = 80,
            density = 2.5f,
            isLandscape = false
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isTrue()
        assertThat(bounds.centerX).isEqualTo(540)
        assertThat(bounds.top).isAtLeast(input.statusBarHeight)
    }

    @Test
    fun `landscape mode hides the overlay`() {
        val input = CutoutSpecs(
            displayWidth = 2400,
            displayHeight = 1080,
            cutoutBoundingRect = Rect(left = 20, top = 490, right = 120, bottom = 590),
            statusBarHeight = 80,
            density = 2.5f,
            isLandscape = true
        )

        val bounds = calculator.calculate(input, isExpanded = false)
        assertThat(bounds.isVisible).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :overlay:test
```
Expected: FAIL (unresolved classes `PositionCalculator`, `CutoutSpecs`).

- [ ] **Step 3: Implement `PositionCalculator`**

`overlay/src/main/kotlin/io/selorax/island/overlay/PositionCalculator.kt`:
```kotlin
package io.selorax.island.overlay

public data class Rect(public val left: Int, public val top: Int, public val right: Int, public val bottom: Int) {
    public val width: Int get() = right - left
    public val height: Int get() = bottom - top
    public val centerX: Int get() = left + width / 2
}

public data class CutoutSpecs(
    public val displayWidth: Int,
    public val displayHeight: Int,
    public val cutoutBoundingRect: Rect?,
    public val statusBarHeight: Int,
    public val density: Float,
    public val isLandscape: Boolean
)

public data class PillBounds(
    public val x: Int,
    public val top: Int,
    public val width: Int,
    public val height: Int,
    public val isVisible: Boolean
) {
    public val centerX: Int get() = x + width / 2
}

public class PositionCalculator {
    public fun calculate(specs: CutoutSpecs, isExpanded: Boolean): PillBounds {
        if (specs.isLandscape) {
            return PillBounds(x = 0, top = 0, width = 0, height = 0, isVisible = false)
        }

        val baseCompactWidth = (160 * specs.density).toInt()
        val baseCompactHeight = (38 * specs.density).toInt()
        val baseExpandedWidth = (340 * specs.density).toInt()
        val baseExpandedHeight = (180 * specs.density).toInt()

        val width = if (isExpanded) baseExpandedWidth.coerceAtMost(specs.displayWidth - 32) else baseCompactWidth
        val height = if (isExpanded) baseExpandedHeight else baseCompactHeight

        val centerX = specs.cutoutBoundingRect?.centerX ?: (specs.displayWidth / 2)
        val x = (centerX - width / 2).coerceIn(0, specs.displayWidth - width)

        val top = specs.cutoutBoundingRect?.top ?: specs.statusBarHeight

        return PillBounds(
            x = x,
            top = top,
            width = width,
            height = height,
            isVisible = true
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :overlay:test
```
Expected: PASS.

---

### Task 7: Window Controller (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/WindowController.kt`
- Test: `overlay/src/test/kotlin/io/selorax/island/overlay/WindowControllerTest.kt`

**Interfaces:**
- Consumes: `WindowManager`, `PositionCalculator`
- Produces: `WindowController` with throttled updates (≤ 2 calls per transition)

- [ ] **Step 1: Write unit test `WindowControllerTest`**

`overlay/src/test/kotlin/io/selorax/island/overlay/WindowControllerTest.kt`:
```kotlin
package io.selorax.island.overlay

import android.view.View
import android.view.WindowManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WindowControllerTest {
    private val windowManager = mockk<WindowManager>(relaxed = true)
    private val targetView = mockk<View>(relaxed = true)

    @Test
    fun `attachView invokes addView with correct overlay flags`() {
        val controller = WindowController(windowManager)
        controller.attachView(targetView)

        verify(exactly = 1) {
            windowManager.addView(
                targetView,
                match { params ->
                    val lp = params as WindowManager.LayoutParams
                    lp.type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY &&
                        (lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                }
            )
        }
    }

    @Test
    fun `transition update coalesces to at most 2 layout updates`() {
        val controller = WindowController(windowManager)
        controller.attachView(targetView)

        // Begin transition to expanded
        controller.onTransitionStart(isExpanded = true, bounds = PillBounds(0, 0, 800, 400, true))
        // Multiple intermediate ticks during animation
        controller.onAnimationTick()
        controller.onAnimationTick()
        // Settle transition
        controller.onTransitionEnd(bounds = PillBounds(0, 0, 800, 400, true))

        // Total updateViewLayout calls must be exactly 2 (start and end), ticks ignored
        assertThat(controller.updateCallCount).isEqualTo(2)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :overlay:test
```
Expected: FAIL (unresolved class `WindowController`).

- [ ] **Step 3: Implement `WindowController`**

`overlay/src/main/kotlin/io/selorax/island/overlay/WindowController.kt`:
```kotlin
package io.selorax.island.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

public class WindowController(
    private val windowManager: WindowManager
) {
    private var attachedView: View? = null
    public var updateCallCount: Int = 0
        private set

    public val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 0
    }

    public fun attachView(view: View) {
        if (attachedView == null) {
            attachedView = view
            windowManager.addView(view, layoutParams)
        }
    }

    public fun detachView() {
        attachedView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: Exception) {}
            attachedView = null
        }
    }

    public fun onTransitionStart(isExpanded: Boolean, bounds: PillBounds) {
        applyBounds(bounds)
    }

    public fun onAnimationTick() {
        // ADR-03: No IPC window resize calls during animation frames
    }

    public fun onTransitionEnd(bounds: PillBounds) {
        applyBounds(bounds)
    }

    private fun applyBounds(bounds: PillBounds) {
        val view = attachedView ?: return
        layoutParams.x = bounds.x
        layoutParams.y = bounds.top
        layoutParams.width = if (bounds.isVisible) bounds.width else 0
        layoutParams.height = if (bounds.isVisible) bounds.height else 0

        try {
            windowManager.updateViewLayout(view, layoutParams)
            updateCallCount++
        } catch (_: Exception) {}
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :overlay:test
```
Expected: PASS.

---

### Task 8: Overlay Host Service & Compose View Integration (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/OverlayHost.kt`
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandPill.kt`
- Create: `overlay/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `IslandState`, `WindowController`, `PositionCalculator`
- Produces: Service hosting floating Compose pill in system `WindowManager`

- [ ] **Step 1: Create `IslandPill` Compose view**

`overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandPill.kt`:
```kotlin
package io.selorax.island.overlay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState

@Composable
public fun IslandPill(
    state: IslandState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    Box(
        modifier = modifier
            .wrapContentHeight()
            .background(Color.Black, shape = RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .animateContentSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is IslandState.Compact -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SlotContent(slot = state.leadingSlot)
                    SlotContent(slot = state.trailingSlot)
                }
            }
            is IslandState.Expanded -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = state.title, color = Color.White, fontSize = 16.sp)
                    state.subtitle?.let {
                        Text(text = it, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun SlotContent(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(text = slot.text, color = Color.White, fontSize = 12.sp)
        is IslandSlot.Icon -> Text(text = "●", color = Color.Green, fontSize = 12.sp)
        is IslandSlot.Progress -> Text(text = "${(slot.progress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}
```

- [ ] **Step 2: Create `OverlayHost` Service**

`overlay/src/main/kotlin/io/selorax/island/overlay/OverlayHost.kt`:
```kotlin
package io.selorax.island.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.overlay.ui.IslandPill

public class OverlayHost : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private lateinit var windowController: WindowController
    private var composeView: ComposeView? = null
    private val currentState = mutableStateOf<IslandState>(
        IslandState.Compact(
            leadingSlot = IslandSlot.Icon("battery"),
            trailingSlot = IslandSlot.Text("100%")
        )
    )

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Suspend overlay rendering during screen off
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Resume overlay rendering
                }
            }
        }
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowController = WindowController(windowManager)

        val view = ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@OverlayHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayHost)
            ViewTreeViewModelStoreOwner.set(this, this@OverlayHost)
            setContent {
                IslandPill(
                    state = currentState.value,
                    onClick = {
                        currentState.value = if (currentState.value is IslandState.Compact) {
                            IslandState.Expanded(
                                title = "Island Test",
                                subtitle = "Compact -> Expanded Transition",
                                leadingSlot = IslandSlot.Icon("test"),
                                contentSlot = IslandSlot.Text("Details"),
                                actions = emptyList()
                            )
                        } else {
                            IslandState.Compact(
                                leadingSlot = IslandSlot.Icon("battery"),
                                trailingSlot = IslandSlot.Text("100%")
                            )
                        }
                    }
                )
            }
        }

        composeView = view
        windowController.attachView(view)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        windowController.detachView()
        composeView = null
        appViewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
```

- [ ] **Step 3: Register `OverlayHost` in `overlay/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application>
        <service
            android:name=".OverlayHost"
            android:exported="false" />
    </application>
</manifest>
```

- [ ] **Step 4: Verify `:overlay` compiles**

Run:
```bash
./gradlew :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 9: App Module Debug Harness (`:app`)

**Files:**
- Create: `app/src/main/kotlin/io/selorax/island/MainActivity.kt`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `:overlay`
- Produces: Launcher APK that grants permission and toggles `OverlayHost` for P1/P4/P5 verification

- [ ] **Step 1: Create strings resource file**

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Island</string>
    <string name="btn_grant_permission">Grant Overlay Permission</string>
    <string name="btn_start_overlay">Start Overlay Service</string>
    <string name="btn_stop_overlay">Stop Overlay Service</string>
    <string name="permission_granted">Overlay Permission: GRANTED</string>
    <string name="permission_denied">Overlay Permission: DENIED</string>
</resources>
```

- [ ] **Step 2: Create `MainActivity`**

`app/src/main/kotlin/io/selorax/island/MainActivity.kt`:
```kotlin
package io.selorax.island

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.selorax.island.overlay.OverlayHost

public class MainActivity : ComponentActivity() {
    private val hasOverlayPermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermissionState()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (hasOverlayPermission.value) {
                                stringResource(R.string.permission_granted)
                            } else {
                                stringResource(R.string.permission_denied)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!hasOverlayPermission.value) {
                            Button(onClick = { requestOverlayPermission() }) {
                                Text(stringResource(R.string.btn_grant_permission))
                            }
                        } else {
                            Button(onClick = { startOverlayService() }) {
                                Text(stringResource(R.string.btn_start_overlay))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(onClick = { stopOverlayService() }) {
                                Text(stringResource(R.string.btn_stop_overlay))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    private fun updatePermissionState() {
        hasOverlayPermission.value = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        startService(intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        stopService(intent)
    }
}
```

- [ ] **Step 3: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Verify `:app` compiles into APK**

Run:
```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL with `app-debug.apk` produced.

---

### Task 10: End-to-End Build & Milestone 1 Verification

**Files:**
- Create: `docs/prototypes/week1-findings.md`

**Interfaces:**
- Consumes: All 14 modules, test suite
- Produces: Verified build, passing unit tests, and documented findings for P1, P4, P5

- [ ] **Step 1: Run complete build and test suite across all modules**

Run:
```bash
./gradlew check
```
Expected: BUILD SUCCESSFUL, all unit tests passing.

- [ ] **Step 2: Document Milestone 1 Prototype Findings in `docs/prototypes/week1-findings.md`**

Record findings for:
- P1: Camera cutout z-order and placement.
- P4: Window resize coalescing (verified ≤ 2 `updateViewLayout` calls per transition).
- P5: Landscape orientation behavior (auto-collapse and hide).

- [ ] **Step 3: Commit Milestone 1 changes**

Run:
```bash
git add .
git commit -m "feat: complete Milestone 1 foundation and floating overlay"
```
