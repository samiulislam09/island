package io.selorax.island.model

public enum class IslandDesignFace(
    public val id: String,
    public val displayName: String,
    public val badge: String,
    public val description: String
) {
    AMOLED_CLASSIC(
        id = "amoled",
        displayName = "AMOLED Classic",
        badge = "🖤",
        description = "Pitch black capsule with subtle high-contrast rim"
    ),
    CYBER_NEON(
        id = "cyber_neon",
        displayName = "Cyber Neon",
        badge = "⚡",
        description = "Electric neon aura edge glow contour"
    ),
    FROSTED_GLASS(
        id = "frosted_glass",
        displayName = "Frosted Glass",
        badge = "💎",
        description = "Translucent smoky glass with specular edge highlight"
    ),
    AURORA_GRADIENT(
        id = "aurora",
        displayName = "Aurora Horizon",
        badge = "🌌",
        description = "Luminous multi-hue aurora gradient contour"
    ),
    TITANIUM_STEALTH(
        id = "titanium",
        displayName = "Titanium Matte",
        badge = "🛡️",
        description = "Precision chamfered graphite edge with matte core"
    ),
    CAMERA_HALO(
        id = "camera_halo",
        displayName = "Punch Hole Halo",
        badge = "⭕",
        description = "Vibrant illuminated accent halo framing the lens cutout"
    );

    public companion object {
        public fun fromId(id: String?): IslandDesignFace {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AMOLED_CLASSIC
        }
    }
}

public enum class AccentGlowColor(
    public val id: String,
    public val displayName: String,
    public val hexColor: Long,
    public val emoji: String
) {
    CYAN("cyan", "Electric Cyan", 0xFF00E5FF, "🔷"),
    PURPLE("purple", "Neon Violet", 0xFFB388FF, "🟣"),
    GREEN("green", "Matrix Emerald", 0xFF00E676, "🟢"),
    AMBER("amber", "Sunset Amber", 0xFFFF9100, "🟠"),
    ROSE("rose", "Crimson Rose", 0xFFFF2D55, "🔴"),
    PEARL("pearl", "Pure White", 0xFFFFFFFF, "⚪");

    public companion object {
        public fun fromId(id: String?): AccentGlowColor {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CYAN
        }
    }
}
