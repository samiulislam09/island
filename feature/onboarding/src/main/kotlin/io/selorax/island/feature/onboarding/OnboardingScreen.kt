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
