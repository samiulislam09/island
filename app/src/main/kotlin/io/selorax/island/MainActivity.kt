package io.selorax.island

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.selorax.island.overlay.OverlayHost

public class MainActivity : ComponentActivity() {
    private val hasOverlayPermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermissionState()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (hasOverlayPermission.value) {
                                stringResource(R.string.permission_granted)
                            } else {
                                stringResource(R.string.permission_denied)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!hasOverlayPermission.value) {
                            Button(onClick = { requestOverlayPermission() }) {
                                Text(stringResource(R.string.btn_grant_permission))
                            }
                        } else {
                            Button(onClick = { startOverlayService() }) {
                                Text(stringResource(R.string.btn_start_overlay))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(onClick = { stopOverlayService() }) {
                                Text(stringResource(R.string.btn_stop_overlay))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionState()
    }

    private fun updatePermissionState() {
        hasOverlayPermission.value = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        startService(intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayHost::class.java)
        stopService(intent)
    }
}
