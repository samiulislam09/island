package io.selorax.island.overlay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.common.compatibility.ThrottleLevel
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.IslandMotion
import io.selorax.island.engine.LiveActivityManager
import io.selorax.island.model.EventSource
import io.selorax.island.model.IslandPresentation
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState
import io.selorax.island.model.UserIntent
import io.selorax.island.overlay.renderers.ActivityRenderer
import io.selorax.island.overlay.renderers.BatteryRenderer
import io.selorax.island.overlay.renderers.MusicRenderer
import io.selorax.island.overlay.renderers.NotificationRenderer
import io.selorax.island.overlay.renderers.TimerRenderer

public object RendererRegistry {
    private val renderers: Map<EventSource, ActivityRenderer> = mapOf(
        EventSource.MEDIA to MusicRenderer(),
        EventSource.BATTERY to BatteryRenderer(),
        EventSource.NOTIFICATION to NotificationRenderer(),
        EventSource.TIMER to TimerRenderer()
    )

    public fun get(source: EventSource): ActivityRenderer? = renderers[source]
}

public fun shouldRunAnimations(isPlaying: Boolean, throttle: ThrottleLevel): Boolean {
    return isPlaying && throttle == ThrottleLevel.NORMAL
}

public fun resolveMorphAnimationSpec(throttleLevel: ThrottleLevel): FiniteAnimationSpec<IntSize> {
    return if (throttleLevel != ThrottleLevel.NORMAL) {
        tween(durationMillis = 150)
    } else {
        IslandMotion.morphSpring()
    }
}

public fun resolveEffectivePresentation(
    presentation: IslandPresentation,
    throttleLevel: ThrottleLevel
): IslandPresentation {
    return if (throttleLevel == ThrottleLevel.SEVERE_THROTTLE && presentation == IslandPresentation.EXPANDED) {
        IslandPresentation.COMPACT
    } else {
        presentation
    }
}

@Composable
public fun IslandRenderer(
    state: LiveActivityManager.InternalIslandState,
    onIntent: (UserIntent) -> Unit,
    isRedacted: Boolean = false,
    modifier: Modifier = Modifier,
    throttleLevel: ThrottleLevel = ThrottleLevel.NORMAL
) {
    val currentDensity = LocalDensity.current
    val clampedDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceAtMost(1.3f)
    )

    LaunchedEffect(throttleLevel, state.presentation) {
        if (throttleLevel == ThrottleLevel.SEVERE_THROTTLE && state.presentation == IslandPresentation.EXPANDED) {
            onIntent(UserIntent.Collapse)
        }
    }

    val effectivePresentation = resolveEffectivePresentation(state.presentation, throttleLevel)
    val animationSpec = resolveMorphAnimationSpec(throttleLevel)
    val isPlaying = shouldRunAnimations(isPlaying = true, throttle = throttleLevel)

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        val primary = state.primary ?: return@CompositionLocalProvider
        val renderer = RendererRegistry.get(primary.source)

        val cornerRadius = if (effectivePresentation == IslandPresentation.EXPANDED) 28.dp else 24.dp

        Box(
            modifier = modifier
                .wrapContentHeight()
                .background(IslandColors.Surface, RoundedCornerShape(cornerRadius))
                .clickable { onIntent(UserIntent.Tap()) }
                .animateContentSize(animationSpec = animationSpec),
            contentAlignment = Alignment.Center
        ) {
            if (renderer != null) {
                when (effectivePresentation) {
                    IslandPresentation.COMPACT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            renderer.CompactStart(primary)
                            renderer.CompactEnd(primary, isRedacted, isPlaying = isPlaying)
                        }
                    }
                    IslandPresentation.EXPANDED -> {
                        renderer.Expanded(primary, isRedacted, onAction = { actionId ->
                            onIntent(UserIntent.ActionClick(primary.id, actionId))
                        })
                    }
                    IslandPresentation.SPLIT -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            renderer.CompactStart(primary)
                            renderer.CompactEnd(primary, isRedacted, isPlaying = isPlaying)
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
            } else {
                when (effectivePresentation) {
                    IslandPresentation.COMPACT -> RenderFallbackCompact(primary.state)
                    IslandPresentation.EXPANDED -> RenderFallbackExpanded(primary.state)
                    IslandPresentation.SPLIT -> RenderFallbackSplit(primary.state, state.secondary?.state)
                    IslandPresentation.MINIMAL -> RenderFallbackCompact(primary.state)
                }
            }
        }
    }
}

@Composable
private fun RenderFallbackCompact(state: IslandState) {
    if (state is IslandState.Compact) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderFallbackSlot(state.leadingSlot)
            Spacer(modifier = Modifier.width(8.dp))
            RenderFallbackSlot(state.trailingSlot)
        }
    }
}

@Composable
private fun RenderFallbackExpanded(state: IslandState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (state is IslandState.Compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RenderFallbackSlot(state.leadingSlot)
                Spacer(modifier = Modifier.width(12.dp))
                RenderFallbackSlot(state.trailingSlot)
            }
        }
    }
}

@Composable
private fun RenderFallbackSplit(primary: IslandState, secondary: IslandState?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RenderFallbackCompact(primary)
        if (secondary != null) {
            Spacer(modifier = Modifier.width(8.dp))
            RenderFallbackCompact(secondary)
        }
    }
}

@Composable
private fun RenderFallbackSlot(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(text = slot.text, color = Color.White, fontSize = 12.sp)
        is IslandSlot.Icon -> Text(text = "●", color = Color.White, fontSize = 12.sp)
        is IslandSlot.Progress -> Text(text = "${(slot.progress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}
