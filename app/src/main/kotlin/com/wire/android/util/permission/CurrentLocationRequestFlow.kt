/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.util.permission

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.wire.android.ui.home.messagecomposer.location.GeoLocatedAddress
import com.wire.android.util.extension.checkPermission

@Composable
fun rememberCurrentLocationFlow(
    onPermissionAllowed: (GeoLocatedAddress) -> Unit,
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
    private val onPermissionAllowed: (GeoLocatedAddress) -> Unit,
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
private fun getCurrentLocation(onPermissionAllowed: (GeoLocatedAddress) -> Unit, context: Context) {
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
//    val locationCallback: LocationCallback = object : LocationCallback() {
//        override fun onLocationResult(result: LocationResult) {
//            val lastLocation = result.locations.last()
//            val address = Geocoder(context).getFromLocation(lastLocation!!.latitude, lastLocation.longitude, 1).orEmpty()
//            onPermissionAllowed(GeoLocatedAddress(address.firstOrNull(), lastLocation))
//        }
//    }

    //todo implement later the updates of this
//    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(50)).build()
//    locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    locationProvider.lastLocation.addOnSuccessListener { lastLocation ->
        val address = Geocoder(context).getFromLocation(lastLocation!!.latitude, lastLocation.longitude, 1).orEmpty()
        onPermissionAllowed(GeoLocatedAddress(address.firstOrNull(), lastLocation))
    }
}
