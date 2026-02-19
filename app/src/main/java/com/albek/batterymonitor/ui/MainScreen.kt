package com.albek.batterymonitor

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingStart by remember { mutableStateOf(false) }

    var showVoltage by remember { mutableStateOf(mainViewModel.getShowVoltage()) }
    var showCurrent by remember { mutableStateOf(mainViewModel.getShowCurrent()) }
    var showTemperature by remember { mutableStateOf(mainViewModel.getShowTemperature()) }
    var showLevel by remember { mutableStateOf(mainViewModel.getShowLevel()) }

    DisposableEffect(lifecycleOwner, pendingStart) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingStart) {
                if (Settings.canDrawOverlays(context)) {
                    mainViewModel.startOverlayService()
                    pendingStart = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showVoltage,
                    onCheckedChange = {
                        showVoltage = it
                        mainViewModel.setShowVoltage(it)
                    }
                )
                Text(stringResource(R.string.option_voltage))
            }

            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showCurrent,
                    onCheckedChange = {
                        showCurrent = it
                        mainViewModel.setShowCurrent(it)
                    }
                )
                Text(stringResource(R.string.option_current))
            }

            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showTemperature,
                    onCheckedChange = {
                        showTemperature = it
                        mainViewModel.setShowTemperature(it)
                    }
                )
                Text(stringResource(R.string.option_temperature))
            }

            Row(
                modifier = Modifier.padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showLevel,
                    onCheckedChange = {
                        showLevel = it
                        mainViewModel.setShowLevel(it)
                    }
                )
                Text(stringResource(R.string.option_level))
            }

            Button(
                onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        mainViewModel.startOverlayService()
                    } else {
                        pendingStart = true
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.action_start_overlay))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { mainViewModel.stopOverlayService() }) {
                Text(stringResource(R.string.action_stop_overlay))
            }
        }
    }
}
