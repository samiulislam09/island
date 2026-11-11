package io.selorax.island.feature.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

public class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-arm alarms for active timers after reboot
        }
    }
}
