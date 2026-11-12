package io.selorax.island.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandMotion
import io.selorax.island.designsystem.IslandTheme
import io.selorax.island.model.AccentGlowColor
import io.selorax.island.model.IslandDesignFace

@Composable
public fun DesignFacesScreen(
    currentFace: IslandDesignFace,
    currentAccent: AccentGlowColor,
    onSelectFace: (IslandDesignFace) -> Unit,
    onSelectAccent: (AccentGlowColor) -> Unit,
    onTestExpandedFace: (String) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    val accentRgb = IslandTheme.getAccent(currentAccent, colors.isLight)
    val isSimExpanded = remember { mutableStateOf(false) }

    val simWidth by animateDpAsState(
        targetValue = if (isSimExpanded.value) 220.dp else 38.dp,
        animationSpec = IslandMotion.morphSpring(),
        label = "simWidth"
    )
    val simHeight by animateDpAsState(
        targetValue = if (isSimExpanded.value) 60.dp else 28.dp,
        animationSpec = IslandMotion.morphSpring(),
        label = "simHeight"
    )
    val simCorner by animateDpAsState(
        targetValue = if (isSimExpanded.value) 20.dp else 14.dp,
        animationSpec = IslandMotion.morphSpring(),
        label = "simCorner"
    )

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
                .border(1.dp, accentRgb.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "AESTHETICS & MORPHING",
                color = accentRgb,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Design Faces & Themes",
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Customize aesthetic pill styles & expanded card layouts",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Live Interactive Face Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live Face Simulation",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${currentFace.displayName} • ${currentAccent.displayName} (Tap bezel to morph)",
                    color = accentRgb,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Simulated Phone Top Edge with selected Face
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(Color(0xFF0D0D0E))
                        .border(
                            1.dp,
                            Color(0xFF2C2C30),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Speaker grille
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF333336))
                    )

                    // Pill Preview with Face Styling and Spring Morph
                    val (previewBg, previewBorder) = getFaceStyle(currentFace, accentRgb)

                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(width = simWidth, height = simHeight)
                            .clip(RoundedCornerShape(simCorner))
                            .then(previewBg)
                            .border(previewBorder, RoundedCornerShape(simCorner))
                            .clickable { isSimExpanded.value = !isSimExpanded.value },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isSimExpanded.value) {
                            // Camera lens dot
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            )
                        } else {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentFace.badge, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentFace.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Liquid Physics Spring", color = accentRgb, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onToggleExpand,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚡ Test Expand Face on Screen", color = colors.textPrimary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Glow Color Palette
        Text(
            text = "Accent Glow Color",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tint highlights, border halos, and action chip buttons",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentGlowColor.entries.forEach { accent ->
                    val isSelected = currentAccent == accent
                    val color = Color(accent.hexColor)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) color else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSelectAccent(accent) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Design Faces Gallery
        Text(
            text = "Aesthetic Island Faces",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Choose the visual face that fits your phone's screen & notch",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        IslandDesignFace.entries.forEach { face ->
            val isSelected = currentFace == face
            val selectedContainer = if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF1E1E22)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onSelectFace(face) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) selectedContainer else colors.cardBackground
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isSelected) accentRgb else colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = face.badge, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = face.displayName,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentRgb.copy(alpha = if (colors.isLight) 0.14f else 0.2f))
                                        .border(1.dp, accentRgb, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = accentRgb,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = face.description,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Expanded Activity Faces Showcase
        Text(
            text = "Expanded Activity Faces Showcase",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tap any face layout below to test its expanded dynamic animation",
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
                // Row 1: Music & Charging
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onTestExpandedFace("music") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🎵 Music Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTestExpandedFace("charging") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⚡ 45W Charging", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Timer & Voice Call
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onTestExpandedFace("timer") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⏱ Monospace Timer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTestExpandedFace("call") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📞 Incoming Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Navigation & Message
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onTestExpandedFace("navigation") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🧭 Turn Navigation", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTestExpandedFace("whatsapp") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💬 Message Preview", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun getFaceStyle(
    face: IslandDesignFace,
    accentRgb: Color
): Pair<Modifier, BorderStroke> {
    return when (face) {
        IslandDesignFace.AMOLED_CLASSIC -> {
            Modifier.background(Color.Black) to
                BorderStroke(0.6.dp, Color(0xFF282828))
        }
        IslandDesignFace.CYBER_NEON -> {
            Modifier.background(Color(0xFF040407)) to
                BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(accentRgb, Color(0xFFB388FF), accentRgb)))
        }
        IslandDesignFace.FROSTED_GLASS -> {
            Modifier.background(Color(0xE6141418)) to
                BorderStroke(0.8.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x18FFFFFF))))
        }
        IslandDesignFace.AURORA_GRADIENT -> {
            Modifier.background(Color(0xFF08080C)) to
                BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFB388FF), Color(0xFFFF2D55), Color(0xFF00E676))))
        }
        IslandDesignFace.TITANIUM_STEALTH -> {
            Modifier.background(Color(0xFF161619)) to
                BorderStroke(1.0.dp, Color(0xFF3F3F46))
        }
        IslandDesignFace.CAMERA_HALO -> {
            Modifier.background(Color.Black) to
                BorderStroke(1.5.dp, accentRgb)
        }
    }
}
