package com.wire.android.util.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.util.extension.checkPermission

@Composable
fun rememberCurrentLocationFlow(
    context: Context,
    onLocationPicked: () -> Unit, // TODO: this will change accordingly to maps intent
    onPermissionDenied: () -> Unit
): CurrentLocationRequestFlow {

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // TODO: launch map location picker using openstreetmap? (have in mind f-droid aka. no gms)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        CurrentLocationRequestFlow(context, requestPermissionLauncher)
    }
}

class CurrentLocationRequestFlow(
    private val context: Context,
    private val locationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // TODO: launch map location picker using openstreetmap? (have in mind f-droid aka. no gms)
        } else {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
