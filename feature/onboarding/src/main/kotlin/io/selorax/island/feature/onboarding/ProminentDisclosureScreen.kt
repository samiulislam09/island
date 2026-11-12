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
