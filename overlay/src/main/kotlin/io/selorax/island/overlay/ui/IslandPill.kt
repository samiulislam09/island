package io.selorax.island.overlay.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.IslandMotion
import io.selorax.island.designsystem.components.EqualizerBars
import io.selorax.island.designsystem.components.IslandProgressBar
import io.selorax.island.designsystem.components.RingProgress
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace
import io.selorax.island.model.IslandSlot
import io.selorax.island.model.IslandState

@Composable
public fun IslandPill(
    state: IslandState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactWidth: Dp = 38.dp,
    compactHeight: Dp = 28.dp,
    designFace: IslandDesignFace = IslandDesignFace.AMOLED_CLASSIC,
    accentColor: AccentGlowColor = AccentGlowColor.CYAN,
    onAction: (String) -> Unit = {}
) {
    if (!state.isVisible) return

    val isExpanded = state is IslandState.Expanded

    // Preserve the last expanded state so during collapse the card smoothly dissolves into the notch origin
    val lastExpandedState = remember { mutableStateOf<IslandState.Expanded?>(null) }
    if (state is IslandState.Expanded) {
        lastExpandedState.value = state
    }
    val activeExpanded = if (state is IslandState.Expanded) state else lastExpandedState.value

    val faceType = if (activeExpanded != null) {
        detectExpandedFaceType(activeExpanded)
    } else {
        ExpandedFaceType.NOTIFICATION
    }

    // Dynamic width and height tailored to the content face
    val targetWidth = if (isExpanded) 340.dp else compactWidth
    val targetHeight = if (isExpanded) {
        when (faceType) {
            ExpandedFaceType.CHARGING -> 92.dp
            ExpandedFaceType.NAVIGATION -> 92.dp
            ExpandedFaceType.TIMER -> 108.dp
            ExpandedFaceType.CALL -> 132.dp
            ExpandedFaceType.MUSIC -> 146.dp
            ExpandedFaceType.NOTIFICATION -> 138.dp
        }
    } else {
        compactHeight
    }
    val targetCorner = if (isExpanded) 28.dp else (compactHeight / 2)

    // Liquid organic physics spring specs from IslandMotion
    val morphSpring = IslandMotion.morphSpring<Dp>()

    val animWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = morphSpring,
        label = "islandWidth"
    )
    val animHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = morphSpring,
        label = "islandHeight"
    )
    val animCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = morphSpring,
        label = "islandCorner"
    )

    // Staggered Content Alpha & Scale for organic fluid reveal and collapse
    val compactAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (isExpanded) 60 else 180,
            delayMillis = if (isExpanded) 0 else 160
        ),
        label = "compactAlpha"
    )
    val compactScale by animateFloatAsState(
        targetValue = if (isExpanded) 0.60f else 1f,
        animationSpec = tween(durationMillis = if (isExpanded) 80 else 180),
        label = "compactScale"
    )

    val expandedAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isExpanded) 200 else 140,
            delayMillis = if (isExpanded) 70 else 0
        ),
        label = "expandedAlpha"
    )
    val expandedScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.40f,
        animationSpec = if (isExpanded) IslandMotion.contentSpring() else tween(140),
        label = "expandedScale"
    )

    val shape = RoundedCornerShape(animCorner)
    val accentRgb = Color(accentColor.hexColor)

    // Compute Face-specific background and border brush
    val (bgModifier, borderStroke) = when (designFace) {
        IslandDesignFace.AMOLED_CLASSIC -> {
            Modifier.background(Color.Black) to
                BorderStroke(
                    width = if (isExpanded) 0.8.dp else 0.5.dp,
                    color = if (isExpanded) Color(0xFF333333) else Color(0xFF242426)
                )
        }
        IslandDesignFace.CYBER_NEON -> {
            Modifier.background(Color(0xFF040407)) to
                BorderStroke(
                    width = if (isExpanded) 1.2.dp else 1.0.dp,
                    brush = Brush.horizontalGradient(
                        listOf(accentRgb, Color(0xFFB388FF), accentRgb)
                    )
                )
        }
        IslandDesignFace.FROSTED_GLASS -> {
            Modifier.background(Color(0xE6141418)) to
                BorderStroke(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x66FFFFFF), Color(0x18FFFFFF))
                    )
                )
        }
        IslandDesignFace.AURORA_GRADIENT -> {
            Modifier.background(Color(0xFF08080C)) to
                BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFFB388FF), Color(0xFFFF2D55), Color(0xFF00E676))
                    )
                )
        }
        IslandDesignFace.TITANIUM_STEALTH -> {
            Modifier.background(Color(0xFF161619)) to
                BorderStroke(
                    width = 1.0.dp,
                    color = Color(0xFF3F3F46)
                )
        }
        IslandDesignFace.CAMERA_HALO -> {
            Modifier.background(Color.Black) to
                BorderStroke(
                    width = if (isExpanded) 1.0.dp else 1.5.dp,
                    color = accentRgb
                )
        }
    }

    Box(
        modifier = modifier
            .width(animWidth)
            .height(animHeight)
            .clip(shape)
            .then(bgModifier)
            .border(borderStroke, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        // Expanded Card View - Anchored to the notch origin and scales/fades smoothly into it
        if (expandedAlpha > 0.01f && activeExpanded != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        alpha = expandedAlpha
                        scaleX = expandedScale
                        scaleY = expandedScale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                when (faceType) {
                    ExpandedFaceType.MUSIC -> MusicFaceContent(activeExpanded, accentRgb, onClick, onAction)
                    ExpandedFaceType.CHARGING -> ChargingFaceContent(activeExpanded, accentRgb, onClick, onAction)
                    ExpandedFaceType.TIMER -> TimerFaceContent(activeExpanded, accentRgb, onClick, onAction)
                    ExpandedFaceType.CALL -> CallFaceContent(activeExpanded, accentRgb, onClick, onAction)
                    ExpandedFaceType.NAVIGATION -> NavigationFaceContent(activeExpanded, accentRgb, onClick, onAction)
                    ExpandedFaceType.NOTIFICATION -> StandardNotificationFaceContent(activeExpanded, accentRgb, onClick, onAction)
                }
            }
        }

        // Compact Punch Hole View - Rock-solid FIXED at the physical hardware notch main point
        if (compactAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compactHeight)
                    .graphicsLayer {
                        alpha = compactAlpha
                        scaleX = compactScale
                        scaleY = compactScale
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    },
                contentAlignment = Alignment.Center
            ) {
                val compactState = (state as? IslandState.Compact)
                val icon = when (val slot = compactState?.leadingSlot) {
                    is IslandSlot.Icon -> when (slot.iconName.lowercase()) {
                        "charging" -> "⚡"
                        "music" -> "🎵"
                        "timer" -> "⏱"
                        "call" -> "📞"
                        "navigation" -> "🧭"
                        else -> ""
                    }
                    else -> ""
                }
                if (icon.isNotEmpty()) {
                    Text(
                        text = icon,
                        color = when (icon) {
                            "⚡", "📞" -> Color(0xFF00E676)
                            "🎵" -> Color(0xFFFF2D55)
                            "⏱" -> Color(0xFFFF9500)
                            else -> accentRgb
                        },
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private enum class ExpandedFaceType {
    MUSIC, CHARGING, TIMER, CALL, NAVIGATION, NOTIFICATION
}

private fun detectExpandedFaceType(state: IslandState.Expanded): ExpandedFaceType {
    val iconName = (state.leadingSlot as? IslandSlot.Icon)?.iconName?.lowercase() ?: ""
    val titleLower = state.title.lowercase()
    return when {
        iconName in listOf("music", "media") || titleLower.contains("music") || titleLower.contains("rhapsody") -> ExpandedFaceType.MUSIC
        iconName in listOf("charging", "battery") || titleLower.contains("charging") || titleLower.contains("unplugged") -> ExpandedFaceType.CHARGING
        iconName in listOf("timer", "stopwatch") || titleLower.contains("timer") -> ExpandedFaceType.TIMER
        iconName in listOf("call", "phone", "dialer") || titleLower.contains("call") -> ExpandedFaceType.CALL
        iconName in listOf("navigation", "maps") || titleLower.contains("navigation") || titleLower.contains("turn") -> ExpandedFaceType.NAVIGATION
        else -> ExpandedFaceType.NOTIFICATION
    }
}

@Composable
private fun MusicFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♫", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.subtitle ?: "Now Playing",
                        color = Color(0xFFB0B0B5),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            EqualizerBars(isPlaying = true)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        IslandProgressBar(progress = 0.42f)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("02:14", color = Color(0xFF8E8E93), fontSize = 10.sp)
            Text("05:54", color = Color(0xFF8E8E93), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Playback control chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillActionChip("⏮", "Prev", Color(0xFF242426), Color.White) { onAction("prev") }
            PillActionChip("⏸", "Pause", accentRgb, Color.Black) { onAction("pause") }
            PillActionChip("⏭", "Next", Color(0xFF242426), Color.White) { onAction("next") }
        }
    }
}

@Composable
private fun ChargingFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RingProgress(progress = 0.85f, size = 48.dp, color = Color(0xFF00E676))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.subtitle ?: "Super Fast Charging 2.0",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "⚡ 45W • 22 minutes until full",
                color = Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
        PillActionChip("✓", "Done", Color(0xFF242426), Color.White) { onAction("dismiss") }
    }
}

@Composable
private fun TimerFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(progress = 0.65f, size = 36.dp, color = Color(0xFFFF9500))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "04:59.2",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = state.subtitle ?: "Timer Running",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp
                    )
                }
            }

            Row {
                PillActionChip("⏸", "Pause", Color(0xFF242426), Color.White) { onAction("pause") }
                Spacer(modifier = Modifier.width(6.dp))
                PillActionChip("⏹", "Stop", Color(0xFFFF3B30), Color.White) { onAction("dismiss") }
            }
        }
    }
}

@Composable
private fun CallFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📞", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.subtitle ?: "Incoming Call...",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            PillActionChip("🔴", "Decline", Color(0xFFD32F2F), Color.White) { onAction("dismiss") }
            Spacer(modifier = Modifier.width(8.dp))
            PillActionChip("🟢", "Answer", Color(0xFF2E7D32), Color.White) { onAction("answer") }
        }
    }
}

@Composable
private fun NavigationFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentRgb),
            contentAlignment = Alignment.Center
        ) {
            Text("↰", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Turn Left in 250m",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Grand Avenue • 12 min (4.2 km)",
                color = Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
        PillActionChip("✕", "Exit", Color(0xFF242426), Color.White) { onAction("dismiss") }
    }
}

@Composable
private fun StandardNotificationFaceContent(
    state: IslandState.Expanded,
    accentRgb: Color,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row: Icon Badge + Title + Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    state.leadingSlot?.let {
                        SlotContent(slot = it)
                    } ?: Text("🔔", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Notification • now",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp
                    )
                }
            }

            // Close pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .clickable { onClose() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Body Message
        state.subtitle?.let { bodyText ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = bodyText,
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Custom Content Slot if any
        state.contentSlot?.let {
            Spacer(modifier = Modifier.height(8.dp))
            SlotContent(slot = it)
        }

        // Interactive Action Buttons
        if (state.actions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                state.actions.forEachIndexed { index, action ->
                    if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                    val isPrimary = index == state.actions.lastIndex
                    PillActionChip(
                        icon = null,
                        label = action.label,
                        bg = if (isPrimary) accentRgb else Color(0xFF242426),
                        textColor = if (isPrimary) Color.Black else Color.White
                    ) {
                        onAction(action.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun PillActionChip(
    icon: String?,
    label: String,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(text = icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SlotContent(slot: IslandSlot) {
    when (slot) {
        is IslandSlot.Text -> Text(
            text = slot.text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        is IslandSlot.Icon -> {
            val iconText = when (slot.iconName.lowercase()) {
                "battery", "charging" -> "⚡"
                "music" -> "🎵"
                "timer" -> "⏱"
                "call" -> "📞"
                "navigation" -> "🧭"
                "alert", "warning" -> "⚠"
                "whatsapp", "com.whatsapp" -> "💬"
                "telegram", "org.telegram.messenger" -> "✈"
                "mail", "com.google.android.gm" -> "✉"
                else -> "●"
            }
            val iconColor = when (slot.iconName.lowercase()) {
                "battery", "charging" -> Color(0xFF00E676)
                "music" -> Color(0xFFFF2D55)
                "timer" -> Color(0xFFFF9500)
                "call" -> Color(0xFF00E676)
                "whatsapp", "com.whatsapp" -> Color(0xFF25D366)
                "telegram", "org.telegram.messenger" -> Color(0xFF0088CC)
                else -> IslandColors.AccentIon
            }
            Text(text = iconText, color = iconColor, fontSize = 13.sp)
        }
        is IslandSlot.Progress -> Text(
            text = "${(slot.progress * 100).toInt()}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
