package io.selorax.island.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.selorax.island.common.compatibility.OemRestrictionProfile
import io.selorax.island.designsystem.IslandColors

@Composable
public fun OemGuidanceCard(
    profile: OemRestrictionProfile,
    onOpenSettings: (OemRestrictionProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(IslandColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, IslandColors.AccentIon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = profile.title, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = profile.description, color = IslandColors.OnSurfaceMedium, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        profile.instructions.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                color = IslandColors.OnSurfaceHigh,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onOpenSettings(profile) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentIon)
        ) {
            Text("Open Settings", color = Color.White)
        }
    }
}
