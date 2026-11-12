package io.selorax.island.common.compatibility

public object OemRegistry {
    private val profiles = mapOf(
        OemType.XIAOMI to OemRestrictionProfile(
            oemType = OemType.XIAOMI,
            title = "Xiaomi / HyperOS Setup",
            description = "Xiaomi restricts background overlays and notification listeners by default.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Enable 'Autostart' for Island.",
                "Go to 'Other permissions' and allow 'Display pop-up windows while running in the background'."
            ),
            settingsPackage = "com.miui.securitycenter",
            settingsComponent = "com.miui.permcenter.permissions.PermissionsEditorActivity"
        ),
        OemType.SAMSUNG to OemRestrictionProfile(
            oemType = OemType.SAMSUNG,
            title = "Samsung One UI Battery Settings",
            description = "One UI may put Island to sleep when unused in the foreground.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Set Battery usage to 'Unrestricted'.",
                "Ensure Island is removed from 'Sleeping apps' in Device Care."
            ),
            settingsIntentAction = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
        ),
        OemType.OPPO_ONEPLUS to OemRestrictionProfile(
            oemType = OemType.OPPO_ONEPLUS,
            title = "OnePlus / ColorOS Background Management",
            description = "OxygenOS/ColorOS restricts background floating windows and auto-launch.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Enable 'Auto-launch' and 'Background activity'.",
                "Allow 'Floating windows' permission."
            ),
            settingsPackage = "com.coloros.safecenter"
        ),
        OemType.VIVO to OemRestrictionProfile(
            oemType = OemType.VIVO,
            title = "Vivo / FuntouchOS Setup",
            description = "FuntouchOS requires manual background authorization.",
            instructions = listOf(
                "Tap 'Open Settings' below.",
                "Set background power consumption to 'High Background Power Usage'."
            ),
            settingsPackage = "com.vivo.permissionmanager"
        )
    )

    public fun getProfile(oemType: OemType): OemRestrictionProfile? = profiles[oemType]
}
