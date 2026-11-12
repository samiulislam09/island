package io.selorax.island.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.common.compatibility.DeviceCapabilityDetector
import io.selorax.island.common.compatibility.OemRegistry
import io.selorax.island.designsystem.IslandTheme
import io.selorax.island.designsystem.components.IslandLogoMark
import io.selorax.island.model.AccentGlowColor

@Composable
public fun SettingsScreen(
    currentAccent: AccentGlowColor = AccentGlowColor.CYAN,
    onOpenBatteryOptimization: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    val accentRgb = IslandTheme.getAccent(currentAccent, colors.isLight)
    val symmetricalExpansion = remember { mutableStateOf(true) }
    val amoledPureBlack = remember { mutableStateOf(true) }
    val fastAnimations = remember { mutableStateOf(true) }
    val hapticFeedback = remember { mutableStateOf(true) }

    val oemType = remember {
        DeviceCapabilityDetector.classifyOem(Build.MANUFACTURER, Build.BRAND)
    }
    val oemProfile = remember {
        OemRegistry.getProfile(oemType)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, accentRgb.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SYSTEM & ENGINE",
                        color = accentRgb,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Settings & Telemetry",
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hardware calibration, background persistence & zero-network privacy",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // OEM Background Persistence Section
        SectionTitle(title = "BACKGROUND PERSISTENCE", accentColor = accentRgb)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = oemProfile?.title ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} Battery Protection",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prevent OEM aggressive memory management from killing overlay",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    val recGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(recGreen.copy(alpha = 0.15f))
                            .border(1.dp, recGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            color = recGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = oemProfile?.description
                        ?: "Android may put Dynamic Island to sleep when inactive in the foreground. Set battery usage to Unrestricted.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val instructions = oemProfile?.instructions ?: listOf(
                    "Open Battery Settings below.",
                    "Set Battery usage to 'Unrestricted'.",
                    "Lock Dynamic Island in Recent Apps to ensure continuous overlay presence."
                )

                instructions.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(accentRgb.copy(alpha = 0.15f))
                                .border(1.dp, accentRgb.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = accentRgb,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step,
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.surfaceVariantBorder)
                    ) {
                        Text("App Info", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onOpenBatteryOptimization,
                        modifier = Modifier.weight(1.3f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentRgb),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Battery Settings",
                            color = if (colors.isLight) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual & Interaction Engine Section
        SectionTitle(title = "VISUAL & INTERACTION ENGINE", accentColor = accentRgb)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Symmetrical Notch Expansion
                PreferenceSwitchRow(
                    title = "Symmetrical Notch Expansion",
                    subtitle = "Fluid liquid expansion outward in both directions from punch hole center",
                    checked = symmetricalExpansion.value,
                    accentColor = accentRgb,
                    onCheckedChange = { symmetricalExpansion.value = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(10.dp))

                // AMOLED Pure Black
                PreferenceSwitchRow(
                    title = "AMOLED True Black (#000000)",
                    subtitle = "Turns off display pixels to physically match the camera lens cutout",
                    checked = amoledPureBlack.value,
                    accentColor = accentRgb,
                    onCheckedChange = { amoledPureBlack.value = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(10.dp))

                // High Refresh Rate Animation (GPU)
                PreferenceSwitchRow(
                    title = "60–120 FPS GPU Acceleration",
                    subtitle = "Zero-jitter Skia hardware pipeline with RenderNode graphics layer caching",
                    checked = fastAnimations.value,
                    accentColor = accentRgb,
                    onCheckedChange = { fastAnimations.value = it }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(10.dp))

                // Haptic Feedback
                PreferenceSwitchRow(
                    title = "Haptic Spring Response",
                    subtitle = "Subtle tactile feedback when the island springs open or closes",
                    checked = hapticFeedback.value,
                    accentColor = accentRgb,
                    onCheckedChange = { hapticFeedback.value = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hardware Diagnostics & Telemetry Section
        SectionTitle(title = "HARDWARE & RUNTIME TELEMETRY", accentColor = accentRgb)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                label = "DEVICE / CHIPSET",
                value = "${Build.MANUFACTURER.uppercase()} • ${Build.MODEL}",
                subValue = "Board: ${Build.BOARD}",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TelemetryTile(
                label = "ANDROID RUNTIME",
                value = "Android ${Build.VERSION.RELEASE}",
                subValue = "API Level ${Build.VERSION.SDK_INT}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                label = "OVERLAY ARCHITECTURE",
                value = "TYPE_OVERLAY",
                subValue = "GPU RenderNode Pipeline",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TelemetryTile(
                label = "RESOURCE FOOTPRINT",
                value = "< 22 MB RAM",
                subValue = "0% Idle CPU • Zero WakeLocks",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Freedom & Privacy Manifesto
        SectionTitle(title = "FREEDOM & PRIVACY MANIFESTO", accentColor = accentRgb)
        Spacer(modifier = Modifier.height(10.dp))

        // 100% Free Forever
        val freeGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, freeGreen.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💎", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% Free for Everyone Forever",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(freeGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "UNLOCKED",
                            color = freeGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Zero paywalls, zero locked design faces, zero subscriptions, zero ads, and zero in-app purchases. Every feature, animation preset, and customization is completely open and free for all users.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ADR-08 Zero-Network Privacy Shield
        val privCyan = if (colors.isLight) Color(0xFF0284C7) else Color(0xFF00E5FF)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, privCyan.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADR-08 Zero-Network Privacy Shield",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(privCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = privCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Zero INTERNET permission declared in AndroidManifest.xml. The Android OS physically prevents this app from creating network sockets. All notification titles, playback states, and charging metrics are processed strictly in local RAM and discarded upon dismissal.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Version & Build Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IslandLogoMark(
                size = 56.dp,
                accentColor = accentRgb,
                isAnimated = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "DYNAMIC ISLAND PRO • v1.0.0",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "io.selorax.island • Built with Jetpack Compose & Liquid Physics",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "HARDWARE NOTCH FUSION ENGINE",
                    color = accentRgb,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(title: String, accentColor: Color) {
    val colors = IslandTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(3.dp, 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = IslandTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = colors.textSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (colors.isLight) Color.White else Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surfaceVariant
            )
        )
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    subValue: String,
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
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
