package io.selorax.island.ui

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandTheme

@Composable
public fun NotchCalibrationScreen(
    topOffset: Float,
    horizontalOffset: Float,
    width: Float,
    height: Float,
    onTopOffsetChange: (Float) -> Unit,
    onHorizontalOffsetChange: (Float) -> Unit,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onApplyPreset: (top: Float, horizontal: Float, width: Float, height: Float) -> Unit,
    onTestExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    val accentCyan = if (colors.isLight) Color(0xFF0284C7) else Color(0xFF00E5FF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Screen Header
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, accentCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "GEOMETRY CALIBRATION",
                color = accentCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Notch Calibration Studio",
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Precision pixel-level alignment with your hardware camera cutout",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Visual Notch Simulator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live Cutout Simulation",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sub-pixel overlay bounds centered over front lens",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Simulated Phone Top Edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(Color(0xFF0D0D0E))
                        .border(
                            1.dp,
                            Color(0xFF2C2C30),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Speaker grille indicator
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF333336))
                    )

                    // Overlay pill representation positioned with user offsets
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (horizontalOffset * 0.8f).dp,
                                y = (topOffset * 1.0f).dp
                            )
                            .size(width = width.dp, height = height.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.Black)
                            .border(1.dp, accentCyan.copy(alpha = 0.7f), RoundedCornerShape(percent = 50)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Camera lens dot inside pill
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF0F172A), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${width.toInt()} × ${height.toInt()} dp • Top: ${topOffset.toInt()} dp • Center: ${if (horizontalOffset.toInt() == 0) "0 (Centered)" else "${horizontalOffset.toInt()} dp"}",
                        color = accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Precision Geometry Sliders with [-] [+] Steppers
        Text(
            text = "PRECISION POSITIONING & DIMENSIONS",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Vertical Top Offset
                CalibrationSliderWithNudge(
                    title = "Vertical Position (Top Offset)",
                    value = topOffset,
                    range = 0f..30f,
                    displayValue = "${topOffset.toInt()} dp",
                    onValueChange = onTopOffsetChange
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal Offset
                CalibrationSliderWithNudge(
                    title = "Horizontal Offset (Left / Right)",
                    value = horizontalOffset,
                    range = -60f..60f,
                    displayValue = if (horizontalOffset.toInt() == 0) "0 dp" else "${horizontalOffset.toInt()} dp",
                    onValueChange = onHorizontalOffsetChange
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(12.dp))

                // Pill Width
                CalibrationSliderWithNudge(
                    title = "Punch Hole Pill Width",
                    value = width,
                    range = 24f..80f,
                    displayValue = "${width.toInt()} dp",
                    onValueChange = onWidthChange
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                Spacer(modifier = Modifier.height(12.dp))

                // Pill Height
                CalibrationSliderWithNudge(
                    title = "Punch Hole Pill Height",
                    value = height,
                    range = 20f..36f,
                    displayValue = "${height.toInt()} dp",
                    onValueChange = onHeightChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Notch Presets
        Text(
            text = "HARDWARE PRESETS",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
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
                    PresetButton(
                        label = "Samsung Center Hole",
                        sub = "38 × 28 dp",
                        color = accentCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        onApplyPreset(8f, 0f, 38f, 28f)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PresetButton(
                        label = "Pixel Cutout",
                        sub = "36 × 28 dp",
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    ) {
                        onApplyPreset(10f, 0f, 36f, 28f)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    PresetButton(
                        label = "Left Corner Hole",
                        sub = "-48 dp offset",
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    ) {
                        onApplyPreset(8f, -48f, 38f, 28f)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PresetButton(
                        label = "Reset Defaults",
                        sub = "Factory specs",
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        onApplyPreset(8f, 0f, 38f, 28f)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Test Live Notch Expansion
        Button(
            onClick = onTestExpand,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "⚡ Test Expansion from Notch",
                color = if (colors.isLight) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CalibrationSliderWithNudge(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    step: Float = 1f
) {
    val colors = IslandTheme.colors
    val accentCyan = if (colors.isLight) Color(0xFF0284C7) else Color(0xFF00E5FF)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stepper [-]
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(6.dp))
                        .clickable { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayValue,
                    color = accentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Stepper [+]
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(6.dp))
                        .clickable { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentCyan,
                activeTrackColor = accentCyan,
                inactiveTrackColor = if (colors.isLight) Color(0xFFCBD5E1) else Color(0xFF2C2C2E)
            )
        )
    }
}

@Composable
private fun PresetButton(
    label: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = IslandTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.surfaceVariantBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sub, color = colors.textSecondary, fontSize = 10.sp)
        }
    }
}
