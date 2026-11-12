package io.selorax.island.feature.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.selorax.island.common.compatibility.OemRestrictionProfile

public object OemIntentLauncher {

    public fun createLaunchIntent(context: Context, profile: OemRestrictionProfile): Intent {
        val pkg = profile.settingsPackage
        val component = profile.settingsComponent
        val action = profile.settingsIntentAction

        val customIntent = when {
            pkg != null && component != null ->
                Intent().setComponent(ComponentName(pkg, component))
            pkg != null ->
                context.packageManager?.getLaunchIntentForPackage(pkg)
            action != null ->
                Intent(action)
            else -> null
        }
        return (customIntent ?: fallbackIntent(context)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    public fun launchSafely(context: Context, profile: OemRestrictionProfile): Boolean {
        val intent = createLaunchIntent(context, profile)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            tryFallback(context)
        } catch (_: SecurityException) {
            tryFallback(context)
        } catch (_: Exception) {
            tryFallback(context)
        }
    }

    public fun fallbackIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun tryFallback(context: Context): Boolean {
        return try {
            context.startActivity(fallbackIntent(context))
            true
        } catch (_: Exception) {
            false
        }
    }
}
