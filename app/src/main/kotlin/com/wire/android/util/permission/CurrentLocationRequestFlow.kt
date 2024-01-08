/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.util.permission

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wire.android.appLogger
import com.wire.android.util.extension.checkPermission
import java.util.concurrent.TimeUnit

@Composable
fun rememberCurrentLocationFlow(
    onPermissionAllowed: (Location?) -> Unit,
    onPermissionDenied: () -> Unit
): CurrentLocationRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionGranted = permissions.all { it.value }
            if (allPermissionGranted) {
                getCurrentLocation(onPermissionAllowed, context)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        CurrentLocationRequestFlow(context, onPermissionAllowed, requestPermissionLauncher)
    }
}

class CurrentLocationRequestFlow(
    private val context: Context,
    private val onPermissionAllowed: (Location?) -> Unit,
    private val locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {
    fun launch() {
        if (checkLocationPermissions(context)) {
            getCurrentLocation(onPermissionAllowed, context)
        } else {
            locationPermissionLauncher.launch(getLocationPermissions())
        }
    }
}

private fun getLocationPermissions() =
    arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

private fun checkLocationPermissions(
    context: Context
): Boolean = context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) &&
        context.checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)

@SuppressLint("MissingPermission")
private fun getCurrentLocation(onPermissionAllowed: (Location?) -> Unit, context: Context) {
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
//    val singleRequest = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
//    locationProvider.getCurrentLocation(singleRequest, null).addOnSuccessListener { location ->
//        appLogger.d("Single Location updated to: $location")
//        onPermissionAllowed(location)
//
//        val address = Geocoder(context).getFromLocation(location.latitude, location.longitude, 1).orEmpty()
//        address.first()?.let {
//            appLogger.d("Single Location: ${it.featureName}, ${it.postalCode}, ${it.countryCode}")
//        }
//    }

    val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            appLogger.d("Regular Location updated to: ${result.lastLocation}")
            onPermissionAllowed(result.lastLocation)

            val address = Geocoder(context).getFromLocation(result.lastLocation!!.latitude, result.lastLocation!!.longitude, 1).orEmpty()
            address.first()?.let {
                appLogger.d("Regular Location: ${it.featureName}, ${it.postalCode}, ${it.countryCode}")
            }
        }
    }
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(50)).build()
    locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    locationProvider.lastLocation.addOnSuccessListener { location ->
        appLogger.d("Last Location updated to: $location")
        onPermissionAllowed(location)

        val address = Geocoder(context).getFromLocation(location.latitude, location.longitude, 1).orEmpty()
        address.first()?.let {
            appLogger.d("Last Location: ${it.featureName}, ${it.postalCode}, ${it.countryCode}")
        }
    }
}
