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
package com.wire.android.ui.home.messagecomposer.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.wire.android.appLogger
import com.wire.android.util.extension.isGoogleServicesAvailable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor() : ViewModel() {

    var state: LocationPickerState by mutableStateOf(LocationPickerState())
        private set

    fun onPermissionsDialogDiscarded() {
        state = state.copy(showPermissionDeniedDialog = false)
    }

    fun onLocationSharingErrorDialogDiscarded() {
        state = state.copy(showLocationSharingError = false)
    }

    fun onPermissionsDenied() {
        state = state.copy(showPermissionDeniedDialog = true)
    }

    private fun toStartLoadingLocationState() {
        state = state.copy(
            showLocationSharingError = false,
            isLocationLoading = true,
            geoLocatedAddress = null
        )
    }

    private fun toLocationLoadedState(geoLocatedAddress: GeoLocatedAddress) {
        state = state.copy(
            showLocationSharingError = false,
            isLocationLoading = false,
            geoLocatedAddress = geoLocatedAddress
        )
    }

    private fun toLocationError() {
        state = state.copy(
            showLocationSharingError = true,
            isLocationLoading = false,
            geoLocatedAddress = null,
        )
    }

    fun getCurrentLocation(context: Context) {
        toStartLoadingLocationState()
        when (context.isGoogleServicesAvailable()) {
            true -> getLocationWithGms(context)
            false -> getLocationWithoutGms(context)
        }
    }

    /**
     * Choosing the best location estimate by docs.
     * https://developer.android.com/develop/sensors-and-location/location/retrieve-current#BestEstimate
     */
    @SuppressLint("MissingPermission")
    private fun getLocationWithGms(context: Context) = viewModelScope.launch {
        appLogger.d("Getting location with GMS")
        if (isLocationServicesEnabled(context)) {
            val locationProvider = LocationServices.getFusedLocationProviderClient(context)
            val currentLocation = locationProvider.getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            val address = Geocoder(context).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).orEmpty()
            toLocationLoadedState(GeoLocatedAddress(address.firstOrNull(), currentLocation))
        } else {
            toLocationError()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWithoutGms(context: Context) = viewModelScope.launch {
        appLogger.d("Getting location without GMS")
        if (isLocationServicesEnabled(context)) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val networkLocationListener: LocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val address = Geocoder(context).getFromLocation(location.latitude, location.longitude, 1).orEmpty()
                    toLocationLoadedState(GeoLocatedAddress(address.firstOrNull(), location))
                    locationManager.removeUpdates(this) // important step, otherwise it will keep listening for location changes
                }
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, networkLocationListener)
        } else {
            toLocationError()
        }
    }

    private fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }
}
