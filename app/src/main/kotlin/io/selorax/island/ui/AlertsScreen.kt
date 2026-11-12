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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.designsystem.IslandTheme

@Composable
public fun AlertsScreen(
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onTestNotification: (title: String, text: String, pkg: String) -> Unit,
    onTestAlert: (type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IslandTheme.colors
    val accentCyan = if (colors.isLight) Color(0xFF0284C7) else Color(0xFF00E5FF)
    val activeGreen = if (colors.isLight) Color(0xFF059669) else Color(0xFF00E676)
    val warningAmber = if (colors.isLight) Color(0xFFD97706) else Color(0xFFFF9500)
    val durationSeconds = remember { mutableFloatStateOf(4.5f) }
    val hideSensitiveOnLock = remember { mutableStateOf(true) }

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
                text = "EVENTS & NOTIFICATIONS",
                color = accentCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Alerts & Event Hub",
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Configure live expansion behaviors, duration, and app triggers",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Notification Access Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            shape = RoundedCornerShape(22.dp),
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
                            text = "Notification Listener Service",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasNotificationPermission)
                                "Active • Intercepting notifications from all apps"
                            else
                                "Permission disabled • Tap below to grant access",
                            color = if (hasNotificationPermission) activeGreen else warningAmber,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasNotificationPermission) activeGreen.copy(alpha = 0.15f) else warningAmber.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (hasNotificationPermission) activeGreen.copy(alpha = 0.4f) else warningAmber.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "ACTIVE" else "DISABLED",
                            color = if (hasNotificationPermission) activeGreen else warningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!hasNotificationPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.divider))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Android 13/14/15 Restricted Setting Guide:",
                        color = warningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If Android displays 'Restricted setting':\n1. Tap Step 1 below to open App Info.\n2. Tap the 3 dots in the top-right corner.\n3. Tap 'Allow restricted settings'.\n4. Tap Step 2 to enable Notification access.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenAppInfo,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.surfaceVariantBorder)
                        ) {
                            Text("1. App Info (3 Dots)", color = colors.textPrimary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("2. Grant Access", color = if (colors.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Expansion Duration Selector
        Text(
            text = "ISLAND EXPANSION DURATION",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "How long the expanded card remains visible before auto-collapsing",
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val durations = listOf(3.0f to "3.0s (Snappy)", 4.5f to "4.5s (Default)", 6.0f to "6.0s (Relaxed)", 8.0f to "8.0s (Extended)")
                durations.forEach { (sec, label) ->
                    val isSelected = durationSeconds.floatValue == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accentCyan else colors.surfaceVariant)
                            .border(1.dp, if (isSelected) accentCyan else colors.surfaceVariantBorder, RoundedCornerShape(10.dp))
                            .clickable { durationSeconds.floatValue = sec }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelected) label.substringBefore(" ") else label.substringBefore(" "),
                            color = if (isSelected) (if (colors.isLight) Color.White else Color.Black) else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy & Behavior Options
        Text(
            text = "BEHAVIOR & LOCKSCREEN PRIVACY",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide message content on lockscreen", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Mask sensitive message body text (ADR-07)", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = hideSensitiveOnLock.value,
                        onCheckedChange = { hideSensitiveOnLock.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (colors.isLight) Color.White else Color.Black,
                            checkedTrackColor = accentCyan,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Event Triggers Grid
        Text(
            text = "LIVE EVENT SIMULATION MATRIX",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Test each specialized expanded face layout in real time",
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
                    AlertTriggerTile(
                        title = "WhatsApp Chat",
                        badge = "💬",
                        subtitle = "Sarah Jenkins",
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
                    AlertTriggerTile(
                        title = "45W Fast Charge",
                        badge = "⚡",
                        subtitle = "USB-C Connected",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("charging")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    AlertTriggerTile(
                        title = "Music Studio",
                        badge = "🎵",
                        subtitle = "Queen • Rhapsody",
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("music")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AlertTriggerTile(
                        title = "Focus Timer",
                        badge = "⏱",
                        subtitle = "05:00 countdown",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("timer")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    AlertTriggerTile(
                        title = "Voice Call",
                        badge = "📞",
                        subtitle = "Incoming audio call",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("call")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AlertTriggerTile(
                        title = "Turn Navigation",
                        badge = "🧭",
                        subtitle = "Turn Left in 250m",
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    ) {
                        onTestAlert("navigation")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertTriggerTile(
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
