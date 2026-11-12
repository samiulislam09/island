package io.selorax.island.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandColors
import io.selorax.island.designsystem.IslandTheme
import io.selorax.island.designsystem.components.IslandLogoMark
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace

@Composable
public fun HomeScreen(
    isRunning: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    currentFace: IslandDesignFace = IslandDesignFace.AMOLED_CLASSIC,
    currentAccent: AccentGlowColor = AccentGlowColor.CYAN,
    isLightMode: Boolean = true,
    onToggleLightMode: (Boolean) -> Unit = {},
    compactWidth: Float = 38f,
    compactHeight: Float = 28f,
    horizontalOffset: Float = 0f,
    onToggleService: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onNavigateToTab: (AppTab) -> Unit = {},
    onTestNotification: (title: String, text: String, pkg: String) -> Unit,
    onTestAlert: (type: String) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    val accentRgb = IslandTheme.getAccent(currentAccent, colors.isLight)
    val showTroubleshootGuide = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top App Header with Brand Logo Mark
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Animated Brand Logo Mark
                IslandLogoMark(
                    size = 46.dp,
                    accentColor = accentRgb,
                    isAnimated = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleExpand() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DYNAMIC ISLAND",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceVariant)
                                .border(1.dp, accentRgb.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = accentRgb,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hardware Notch Integration • Android 14/15",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Light / Dark Theme Mode Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(12.dp))
                    .clickable { onToggleLightMode(!isLightMode) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLightMode) "☀️" else "🌙",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLightMode) "LIGHT" else "DARK",
                        color = colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Live Pulse Status Badge
            val activeGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isRunning) activeGreen.copy(alpha = 0.15f) else colors.surfaceVariant)
                    .border(
                        1.dp,
                        if (isRunning) activeGreen.copy(alpha = 0.45f) else colors.surfaceVariantBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) activeGreen else colors.textMuted)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isRunning) "ACTIVE" else "STANDBY",
                        color = if (isRunning) activeGreen else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Master Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay Engine",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val activeStatusColor = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
                        Text(
                            text = if (isRunning)
                                "Morphing island active over front camera cutout"
                            else
                                "Service paused • Tap switch to activate",
                            color = if (isRunning) activeStatusColor else colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = isRunning,
                        onCheckedChange = { onToggleService(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (colors.isLight) Color.White else Color.Black,
                            checkedTrackColor = accentRgb,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggleExpand,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⚡ Test Notch Morph", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { onNavigateToTab(AppTab.FACES) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.surfaceVariantBorder)
                    ) {
                        Text("Style: ${currentFace.displayName}", color = accentRgb, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Telemetry Tiles (2x2 Grid)
        Text(
            text = "TELEMETRY & HARDWARE ANCHOR",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                title = "CUTOUT GEOMETRY",
                value = "${compactWidth.toInt()} × ${compactHeight.toInt()} dp",
                subtitle = if (horizontalOffset == 0f) "Centered punch hole" else "Offset ${horizontalOffset.toInt()} dp",
                icon = "🎯",
                modifier = Modifier.weight(1f).clickable { onNavigateToTab(AppTab.NOTCH) }
            )
            Spacer(modifier = Modifier.width(10.dp))
            TelemetryTile(
                title = "DESIGN FACE",
                value = currentFace.displayName,
                subtitle = currentAccent.displayName,
                icon = currentFace.badge,
                modifier = Modifier.weight(1f).clickable { onNavigateToTab(AppTab.FACES) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val activeGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                title = "EVENT LISTENER",
                value = if (hasNotificationPermission) "Connected" else "Attention",
                subtitle = if (hasNotificationPermission) "All 3rd-party apps" else "Permission required",
                icon = "🔔",
                valueColor = if (hasNotificationPermission) activeGreen else Color(0xFFFF9500),
                modifier = Modifier.weight(1f).clickable { onNavigateToTab(AppTab.ALERTS) }
            )
            Spacer(modifier = Modifier.width(10.dp))
            TelemetryTile(
                title = "GPU PIPELINE",
                value = "120 FPS",
                subtitle = "FastOutSlowIn Skia",
                icon = "⚡",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Permissions Health Console
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERMISSIONS HEALTH CONSOLE",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (!hasNotificationPermission || !hasOverlayPermission) {
                Text(
                    text = "ACTION REQUIRED",
                    color = if (colors.isLight) Color(0xFFEA580C) else Color(0xFFFF9500),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Overlay Permission Row
                PermissionConsoleRow(
                    title = "System Overlay (Appear on Top)",
                    description = if (hasOverlayPermission) "Active • Floating window renders over camera hole" else "Required to draw Dynamic Island over other apps",
                    isGranted = hasOverlayPermission,
                    onGrant = onRequestOverlayPermission
                )

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(14.dp))

                // Notification Listener Row
                PermissionConsoleRow(
                    title = "Notification Listener Access",
                    description = if (hasNotificationPermission)
                        "Active • Real-time alerts from WhatsApp, Telegram, SMS & all apps"
                    else
                        "Required for incoming message & call expansions",
                    isGranted = hasNotificationPermission,
                    onGrant = onRequestNotificationPermission
                )

                if (!hasNotificationPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (colors.isLight) Color(0xFFFFF7ED) else Color(0x1AFF9500))
                            .border(1.dp, if (colors.isLight) Color(0xFFFED7AA) else Color(0x33FF9500), RoundedCornerShape(10.dp))
                            .clickable { showTroubleshootGuide.value = !showTroubleshootGuide.value }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💡 Seeing 'Restricted setting' on Android 13/14/15?",
                            color = if (colors.isLight) Color(0xFFC2410C) else Color(0xFFFFB74D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (showTroubleshootGuide.value) "Hide ▲" else "Guide ▼",
                            color = if (colors.isLight) Color(0xFFC2410C) else Color(0xFFFFB74D),
                            fontSize = 11.sp
                        )
                    }

                    AnimatedVisibility(visible = showTroubleshootGuide.value) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "To bypass Android's sideload restriction:\n1. Tap 'App Info' below.\n2. Tap the 3 dots in the top-right corner.\n3. Tap 'Allow restricted settings'.\n4. Return here and tap 'Grant Access'.",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenAppInfo,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.surfaceVariantBorder)
                            ) {
                                Text("Open App Info (3 Dots)", color = colors.textPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Quick Event Showcase Grid
        Text(
            text = "QUICK LIVE EVENT SHOWCASE",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Tap to trigger instant hardware notch expansions",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ShowcaseActionCard(
                        title = "WhatsApp",
                        badge = "💬",
                        subtitle = "Chat message",
                        color = Color(0xFF25D366),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestNotification(
                            "Sarah Jenkins",
                            "Hey! Are you free for dinner tonight around 7?",
                            "com.whatsapp"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShowcaseActionCard(
                        title = "Charging",
                        badge = "⚡",
                        subtitle = "Super fast 45W",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("charging")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ShowcaseActionCard(
                        title = "Music Studio",
                        badge = "🎵",
                        subtitle = "Bohemian Rhapsody",
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("music")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShowcaseActionCard(
                        title = "Kitchen Timer",
                        badge = "⏱",
                        subtitle = "05:00 countdown",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("timer")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Diagnostic Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} • ANDROID ${Build.VERSION.RELEASE}",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "100% Offline • Zero Analytics • Zero Internet Permissions",
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(text = icon, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = if (valueColor == Color.Unspecified) colors.textPrimary else valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PermissionConsoleRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val colors = IslandTheme.colors
    val activeGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
    val warningAmber = if (colors.isLight) Color(0xFFD97706) else Color(0xFFFF9500)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) activeGreen.copy(alpha = 0.15f) else warningAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGranted) "✓" else "!",
                        color = if (isGranted) activeGreen else warningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = activeGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val grantAccent = if (colors.isLight) Color(0xFF0284C7) else Color(0xFF00E5FF)
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = grantAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Grant", color = if (colors.isLight) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShowcaseActionCard(
    title: String,
    badge: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = IslandTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
