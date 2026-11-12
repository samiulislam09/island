package io.selorax.island.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

public object IslandTypography {
    public const val tabularNumbers: String = "tnum"
    public val compactTextSize = 12.sp
    public val expandedTitleSize = 16.sp
    public val expandedSubtitleSize = 13.sp

    public val TimerLarge: TextStyle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = tabularNumbers
    )

    public val TimerMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = tabularNumbers
    )
}
