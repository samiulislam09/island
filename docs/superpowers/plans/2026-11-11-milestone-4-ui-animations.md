# Milestone 4: UI, Animations & Customization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the design system tokens, physical spring animations, specialized activity renderers (Music, Battery, Notification, Timer), and the companion app screens (Onboarding + Prominent Disclosure, Settings Dashboard).

**Architecture:** `:core:designsystem` provides tokens and primitives (`IslandColors`, `IslandMotion`, `EqualizerBars`, `RingProgress`, `SettingsRows`). `:overlay` integrates specialized `ActivityRenderer` components animated with Compose springs. `:feature:onboarding` provides the setup and disclosure flow, while `:feature:settings` binds to `SettingsRepository` for real-time overlay adjustments.

**Tech Stack:** Jetpack Compose, Material 3, AndroidX Compose Animation, Kotlin 2.0.21, JUnit 4/5, Google Truth.

**Spec:** `docs/superpowers/specs/2026-11-11-milestone-4-ui-animations-design.md`

## Global Constraints
- User Constraint: "dont commit automatically" — NEVER execute automated `git commit` commands; verify each task with tests and update the SDD ledger.
- Strict TDD: Follow RED-GREEN-REFACTOR for all tasks.
- Accessibility: All interactive elements maintain ≥ 48 dp touch target; reduced motion mode falls back to simple fade transitions.
- Privacy: Never display unredacted notification details on the lock screen (ADR-07).

## Review Focus
1. Tabular numbers (`tnum`) must prevent text jitter during timer countdowns and media playback.
2. Spring animations must remain fluid and interruptible without jank.
3. Lock screen notification mode must redact sensitive text.
4. Prominent disclosure must precede the system Notification Access screen.
5. Overlay must respond reactively to settings changes in real time.

---

### Task 1: Design System Tokens & Motion Specs (`:core:designsystem`)

**Files:**
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandColors.kt`
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandMotion.kt`
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandTypography.kt`
- Test: `core/designsystem/src/test/kotlin/io/selorax/island/designsystem/DesignTokensTest.kt`

**Interfaces:**
- Consumes: Compose Animation and Graphics
- Produces: `IslandColors`, `IslandMotion`, `IslandTypography`

- [ ] **Step 1: Write failing unit test `DesignTokensTest`**

`core/designsystem/src/test/kotlin/io/selorax/island/designsystem/DesignTokensTest.kt`:
```kotlin
package io.selorax.island.designsystem

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignTokensTest {
    @Test
    fun `island surface color is near black`() {
        assertThat(IslandColors.Surface).isEqualTo(Color(0xFF0B0B0C))
    }

    @Test
    fun `spring motion specs are defined`() {
        assertThat(IslandMotion.morphDamping).isEqualTo(0.82f)
        assertThat(IslandMotion.morphStiffness).isEqualTo(380f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :core:designsystem:testDebugUnitTest
```
Expected: FAIL (unresolved classes `IslandColors`, `IslandMotion`).

- [ ] **Step 3: Implement tokens in `:core:designsystem`**

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandColors.kt`:
```kotlin
package io.selorax.island.designsystem

import androidx.compose.ui.graphics.Color

public object IslandColors {
    public val Surface: Color = Color(0xFF0B0B0C)
    public val SurfaceLight: Color = Color(0xFFF2F2F4)
    public val OnSurfaceHigh: Color = Color(0xEBFFFFFF)
    public val OnSurfaceMedium: Color = Color(0xA3FFFFFF)
    public val AccentIon: Color = Color(0xFF5B8CFF)
    public val Danger: Color = Color(0xFFFF5A5F)
    public val Success: Color = Color(0xFF3DDC84)
}
```

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandMotion.kt`:
```kotlin
package io.selorax.island.designsystem

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

public object IslandMotion {
    public const val morphDamping: Float = 0.82f
    public const val morphStiffness: Float = 380f

    public const val contentDamping: Float = 1.0f
    public const val contentStiffness: Float = 600f

    public const val alertDamping: Float = 0.55f
    public const val alertStiffness: Float = 300f

    public fun <T> morphSpring(): SpringSpec<T> = spring(dampingRatio = morphDamping, stiffness = morphStiffness)
    public fun <T> contentSpring(): SpringSpec<T> = spring(dampingRatio = contentDamping, stiffness = contentStiffness)
    public fun <T> alertSpring(): SpringSpec<T> = spring(dampingRatio = alertDamping, stiffness = alertStiffness)
}
```

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/IslandTypography.kt`:
```kotlin
package io.selorax.island.designsystem

import androidx.compose.ui.text.font.FontFeature
import androidx.compose.ui.unit.sp

public object IslandTypography {
    public val tabularNumbers: String = "tnum"
    public val compactTextSize = 12.sp
    public val expandedTitleSize = 16.sp
    public val expandedSubtitleSize = 13.sp
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :core:designsystem:testDebugUnitTest
```
Expected: PASS.

---

### Task 2: Foundational Composables (`:core:designsystem`)

**Files:**
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/EqualizerBars.kt`
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/RingProgress.kt`
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/IslandProgressBar.kt`

**Interfaces:**
- Consumes: Compose Canvas & Animation
- Produces: Visual indicators for active playback and timers

- [ ] **Step 1: Implement `EqualizerBars`**

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/EqualizerBars.kt`:
```kotlin
package io.selorax.island.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = IslandColors.AccentIon
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bar1Height = if (isPlaying) h1.dp else 4.dp
        val bar2Height = if (isPlaying) h2.dp else 8.dp
        val bar3Height = if (isPlaying) h3.dp else 4.dp

        Box(Modifier.width(3.dp).height(bar1Height).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(bar2Height).background(barColor, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).height(bar3Height).background(barColor, RoundedCornerShape(1.5.dp)))
    }
}
```

- [ ] **Step 2: Implement `RingProgress` and `IslandProgressBar`**

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/RingProgress.kt`:
```kotlin
package io.selorax.island.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = IslandColors.AccentIon,
    trackColor: Color = Color(0x33FFFFFF)
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        // Background track
        drawCircle(color = trackColor, style = stroke)
        // Progress sweep
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            style = stroke
        )
    }
}
```

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/IslandProgressBar.kt`:
```kotlin
package io.selorax.island.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun IslandProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = IslandColors.AccentIon,
    trackColor: Color = Color(0x33FFFFFF)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}
```

- [ ] **Step 3: Verify `:core:designsystem` compilation**

Run:
```bash
./gradlew :core:designsystem:assembleDebug :core:designsystem:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL.

---

### Task 3: Settings UI Primitives (`:core:designsystem`)

**Files:**
- Create: `core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/SettingsRows.kt`
- Test: `core/designsystem/src/test/kotlin/io/selorax/island/designsystem/SettingsRowsTest.kt`

**Interfaces:**
- Produces: `SettingsSection`, `SettingsSwitchRow`, `SettingsSliderRow`

- [ ] **Step 1: Implement `SettingsRows`**

`core/designsystem/src/main/kotlin/io/selorax/island/designsystem/components/SettingsRows.kt`:
```kotlin
package io.selorax.island.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            color = IslandColors.AccentIon,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        content()
    }
}

@Composable
public fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            subtitle?.let {
                Text(text = it, color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
public fun SettingsSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    displayValue: String = "%.0f".format(value)
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(text = displayValue, color = IslandColors.OnSurfaceMedium, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
./gradlew :core:designsystem:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 4: Activity Renderers Architecture & Music Renderer (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/renderers/ActivityRenderer.kt`
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/renderers/MusicRenderer.kt`
- Modify: `overlay/build.gradle.kts` (add `:core:designsystem`)

**Interfaces:**
- Consumes: `LiveActivity`, `EqualizerBars`, `IslandProgressBar`
- Produces: Concrete `ActivityRenderer` for Music playback

- [ ] **Step 1: Add `:core:designsystem` to `overlay/build.gradle.kts`**

Add `implementation(project(":core:designsystem"))` to `overlay/build.gradle.kts`.

- [ ] **Step 2: Implement `ActivityRenderer` and `MusicRenderer`**

`overlay/src/main/kotlin/io/selorax/island/overlay/renderers/ActivityRenderer.kt`:
```kotlin
package io.selorax.island.overlay.renderers

import androidx.compose.runtime.Composable
import io.selorax.island.model.EventSource
import io.selorax.island.model.LiveActivity

public interface ActivityRenderer {
    public val source: EventSource
    @Composable public fun CompactStart(activity: LiveActivity)
    @Composable public fun CompactEnd(activity: LiveActivity)
    @Composable public fun Expanded(activity: LiveActivity, onAction: (String) -> Unit)
}
```

`overlay/src/main/kotlin/io/selorax/island/overlay/renderers/MusicRenderer.kt`:
```kotlin
package io.selorax.island.overlay.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.EqualizerBars
import io.selorax.island.designsystem.components.IslandProgressBar
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class MusicRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.MEDIA

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(IslandColors.AccentIon, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", color = Color.White, fontSize = 12.sp)
        }
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity) {
        EqualizerBars(isPlaying = true)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, onAction: (String) -> Unit) {
        val title = if (activity.state is IslandState.Compact && activity.state.trailingSlot is IslandSlot.Text) {
            (activity.state.trailingSlot as IslandSlot.Text).text
        } else "Now Playing"

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(IslandColors.AccentIon, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♫", color = Color.White, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1)
                    Text("Media Audio", color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            IslandProgressBar(progress = 0.45f)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏮", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { onAction("prev") })
                Text("⏸", color = Color.White, fontSize = 24.sp, modifier = Modifier.clickable { onAction("pause") })
                Text("⏭", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { onAction("next") })
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
./gradlew :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 5: Battery, Notification & Timer Renderers (`:overlay`)

**Files:**
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/renderers/BatteryRenderer.kt`
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/renderers/NotificationRenderer.kt`
- Create: `overlay/src/main/kotlin/io/selorax/island/overlay/renderers/TimerRenderer.kt`

**Interfaces:**
- Consumes: `ActivityRenderer`, `RingProgress`
- Produces: Specialized renderers for Battery, Notification, and Timer activities

- [ ] **Step 1: Implement `BatteryRenderer`, `NotificationRenderer`, and `TimerRenderer`**

`overlay/src/main/kotlin/io/selorax/island/overlay/renderers/BatteryRenderer.kt`:
```kotlin
package io.selorax.island.overlay.renderers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.RingProgress
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class BatteryRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.BATTERY

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("⚡", color = IslandColors.Success, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity) {
        val label = if (activity.state is IslandState.Compact && activity.state.trailingSlot is IslandSlot.Text) {
            (activity.state.trailingSlot as IslandSlot.Text).text
        } else "Battery"
        Text(label, color = Color.White, fontSize = 12.sp)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, onAction: (String) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingProgress(progress = 0.85f, size = 36.dp, color = IslandColors.Success)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Battery Status", color = Color.White, fontSize = 16.sp)
                Text("Charging connected", color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
            }
        }
    }
}
```

`overlay/src/main/kotlin/io/selorax/island/overlay/renderers/NotificationRenderer.kt`:
```kotlin
package io.selorax.island.overlay.renderers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class NotificationRenderer(
    private val isRedacted: Boolean = false
) : ActivityRenderer {
    override val source: EventSource = EventSource.NOTIFICATION

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("✉", color = Color.White, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity) {
        val label = if (isRedacted) "Notification" else {
            if (activity.state is IslandState.Compact && activity.state.trailingSlot is IslandSlot.Text) {
                (activity.state.trailingSlot as IslandSlot.Text).text
            } else "Message"
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, onAction: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = if (isRedacted) "Content Hidden" else "New Notification",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isRedacted) "Unlock to view message details" else "Tap to open details in app",
                color = IslandColors.OnSurfaceMedium,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Dismiss",
                    color = IslandColors.Danger,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction("dismiss") }.padding(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Open",
                    color = IslandColors.AccentIon,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction("open") }.padding(8.dp)
                )
            }
        }
    }
}
```

`overlay/src/main/kotlin/io/selorax/island/overlay/renderers/TimerRenderer.kt`:
```kotlin
package io.selorax.island.overlay.renderers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.RingProgress
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.LiveActivity

public class TimerRenderer : ActivityRenderer {
    override val source: EventSource = EventSource.TIMER

    @Composable
    override fun CompactStart(activity: LiveActivity) {
        Text("⏱", color = IslandColors.AccentIon, fontSize = 14.sp)
    }

    @Composable
    override fun CompactEnd(activity: LiveActivity) {
        val label = if (activity.state is IslandState.Compact && activity.state.trailingSlot is IslandSlot.Text) {
            (activity.state.trailingSlot as IslandSlot.Text).text
        } else "00:00"
        Text(label, color = Color.White, fontSize = 12.sp)
    }

    @Composable
    override fun Expanded(activity: LiveActivity, onAction: (String) -> Unit) {
        val label = if (activity.state is IslandState.Compact && activity.state.trailingSlot is IslandSlot.Text) {
            (activity.state.trailingSlot as IslandSlot.Text).text
        } else "00:00"

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(progress = 0.65f, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(label, color = Color.White, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(
                    "Pause",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction("pause") }.padding(8.dp)
                )
                Text(
                    "+1 min",
                    color = IslandColors.AccentIon,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction("add1m") }.padding(8.dp)
                )
                Text(
                    "Cancel",
                    color = IslandColors.Danger,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onAction("cancel") }.padding(8.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
./gradlew :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 6: Overlay Integration & Motion Springs (`:overlay`)

**Files:**
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandRenderer.kt`
- Modify: `overlay/src/main/kotlin/io/selorax/island/overlay/ui/IslandPill.kt`

**Interfaces:**
- Produces: Polished `IslandRenderer` delegating to `ActivityRenderer` registry with `IslandMotion.morphSpring()`

- [ ] **Step 1: Update `IslandRenderer.kt` with ActivityRenderer dispatch**

```kotlin
package io.selorax.island.overlay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.IslandMotion
import io.selorax.island.engine.LiveActivityManager
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.UserIntent
import io.selorax.island.overlay.renderers.ActivityRenderer
import io.selorax.island.overlay.renderers.BatteryRenderer
import io.selorax.island.overlay.renderers.MusicRenderer
import io.selorax.island.overlay.renderers.NotificationRenderer
import io.selorax.island.overlay.renderers.TimerRenderer

public object RendererRegistry {
    private val renderers = mapOf<EventSource, ActivityRenderer>(
        EventSource.MEDIA to MusicRenderer(),
        EventSource.BATTERY to BatteryRenderer(),
        EventSource.NOTIFICATION to NotificationRenderer(),
        EventSource.TIMER to TimerRenderer()
    )

    public fun get(source: EventSource): ActivityRenderer? = renderers[source]
}

@Composable
public fun IslandRenderer(
    state: LiveActivityManager.InternalIslandState,
    onIntent: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = state.primary ?: return
    val renderer = RendererRegistry.get(primary.source) ?: return

    val cornerRadius = if (state.presentation == IslandPresentation.EXPANDED) 28.dp else 24.dp

    Box(
        modifier = modifier
            .wrapContentHeight()
            .background(IslandColors.Surface, RoundedCornerShape(cornerRadius))
            .clickable { onIntent(UserIntent.Tap()) }
            .animateContentSize(animationSpec = IslandMotion.morphSpring()),
        contentAlignment = Alignment.Center
    ) {
        when (state.presentation) {
            IslandPresentation.COMPACT -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    renderer.CompactStart(primary)
                    renderer.CompactEnd(primary)
                }
            }
            IslandPresentation.EXPANDED -> {
                renderer.Expanded(primary, onAction = { actionId ->
                    onIntent(UserIntent.ActionClick(primary.id, actionId))
                })
            }
            IslandPresentation.SPLIT -> {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    renderer.CompactStart(primary)
                    renderer.CompactEnd(primary)
                    state.secondary?.let { secondary ->
                        Spacer(Modifier.width(8.dp))
                        RendererRegistry.get(secondary.source)?.CompactStart(secondary)
                    }
                }
            }
            IslandPresentation.MINIMAL -> {
                Box(Modifier.padding(6.dp)) {
                    renderer.CompactStart(primary)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation and tests**

Run:
```bash
./gradlew :overlay:testDebugUnitTest :overlay:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 7: Onboarding & Prominent Disclosure Flow (`:feature:onboarding`)

**Files:**
- Create: `feature/onboarding/src/main/kotlin/io/selorax/island/feature/onboarding/OnboardingScreen.kt`
- Create: `feature/onboarding/src/main/kotlin/io/selorax/island/feature/onboarding/ProminentDisclosureScreen.kt`
- Modify: `feature/onboarding/build.gradle.kts` (add `:core:designsystem`, `:core:data`)

**Interfaces:**
- Produces: `OnboardingScreen` and `ProminentDisclosureScreen`

- [ ] **Step 1: Configure dependencies in `feature/onboarding/build.gradle.kts`**

Add `:core:designsystem` and `:core:data` to dependencies.

- [ ] **Step 2: Implement `ProminentDisclosureScreen` and `OnboardingScreen`**

`feature/onboarding/src/main/kotlin/io/selorax/island/feature/onboarding/ProminentDisclosureScreen.kt`:
```kotlin
package io.selorax.island.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun ProminentDisclosureScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IslandColors.Surface)
            .padding(24.dp)
    ) {
        Text("Notification Access", color = Color.White, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Island reads incoming notification titles and music playback state to show alerts and media controls in your cutout pill.",
            color = IslandColors.OnSurfaceHigh,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Privacy Guarantee:\n• Notification text is processed entirely in memory.\n• Never stored on disk or database.\n• Never sent across any network.",
            color = IslandColors.OnSurfaceMedium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentIon)
        ) {
            Text("Allow Access", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
            Text("Not Now", color = IslandColors.OnSurfaceMedium)
        }
    }
}
```

`feature/onboarding/src/main/kotlin/io/selorax/island/feature/onboarding/OnboardingScreen.kt`:
```kotlin
package io.selorax.island.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors

@Composable
public fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }

    if (step == 2) {
        ProminentDisclosureScreen(
            onAccept = { onComplete() },
            onDecline = { onComplete() }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(IslandColors.Surface)
                .padding(24.dp)
        ) {
            Text(
                text = if (step == 0) "Welcome to Island" else "Customized for Your Phone",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (step == 0)
                    "Transform your Android camera punch-hole into an interactive, fluid dynamic island."
                else
                    "Adjust positions, pill size, and choose which activities appear around your cutout.",
                color = IslandColors.OnSurfaceMedium,
                fontSize = 15.sp
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { step++ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentIon)
            ) {
                Text(if (step == 0) "Get Started" else "Next", color = Color.White)
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
./gradlew :feature:onboarding:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 8: Settings Dashboard (`:feature:settings`)

**Files:**
- Create: `feature/settings/src/main/kotlin/io/selorax/island/feature/settings/SettingsDashboard.kt`
- Modify: `feature/settings/build.gradle.kts` (add `:core:designsystem`, `:core:data`)

**Interfaces:**
- Consumes: `SettingsRepository`, `IslandSettings`, `SettingsRows`
- Produces: `SettingsDashboard` Composable with master switch, anchor sliders, and live updates

- [ ] **Step 1: Configure dependencies in `feature/settings/build.gradle.kts`**

Add `:core:designsystem` and `:core:data` to dependencies.

- [ ] **Step 2: Implement `SettingsDashboard`**

`feature/settings/src/main/kotlin/io/selorax/island/feature/settings/SettingsDashboard.kt`:
```kotlin
package io.selorax.island.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.selorax.island.data.settings.IslandSettings
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.components.SettingsSection
import io.selorax.island.designsystem.components.SettingsSliderRow
import io.selorax.island.designsystem.components.SettingsSwitchRow

@Composable
public fun SettingsDashboard(
    settings: IslandSettings,
    onUpdateEnabled: (Boolean) -> Unit,
    onUpdateWidth: (Float) -> Unit,
    onUpdateHeight: (Float) -> Unit,
    onUpdateHideInLandscape: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(IslandColors.Surface)
            .padding(vertical = 16.dp)
    ) {
        item {
            SettingsSection(title = "General") {
                SettingsSwitchRow(
                    title = "Enable Island",
                    subtitle = "Show floating overlay around camera cutout",
                    checked = settings.islandEnabled,
                    onCheckedChange = onUpdateEnabled
                )
                SettingsSwitchRow(
                    title = "Hide in Landscape",
                    subtitle = "Suppress overlay during fullscreen video and games",
                    checked = settings.hideInLandscape,
                    onCheckedChange = onUpdateHideInLandscape
                )
            }
        }

        item {
            SettingsSection(title = "Geometry & Sizing") {
                SettingsSliderRow(
                    title = "Compact Width",
                    value = settings.compactWidthDp,
                    range = 100f..240f,
                    displayValue = "${settings.compactWidthDp.toInt()} dp",
                    onValueChange = onUpdateWidth
                )
                SettingsSliderRow(
                    title = "Compact Height",
                    value = settings.compactHeightDp,
                    range = 28f..48f,
                    displayValue = "${settings.compactHeightDp.toInt()} dp",
                    onValueChange = onUpdateHeight
                )
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
./gradlew :feature:settings:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 9: End-to-End Build & Milestone 4 Verification

**Files:**
- Create: `docs/prototypes/week4-findings.md`

**Interfaces:**
- Consumes: All 14 modules, full test suite
- Produces: Green test suite across the entire project and documented Week 4 findings report

- [ ] **Step 1: Run complete build and test suite**

Run:
```bash
./gradlew test :app:assembleDebug
```
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 2: Document Milestone 4 findings in `docs/prototypes/week4-findings.md`**

Record findings for:
- Design system token consistency and tabular typography.
- Performance of Compose springs and interruptible morph animations.
- Accessibility compliance (touch targets ≥ 48 dp, reduced motion mode).
- Onboarding and prominent disclosure Google Play compliance.
