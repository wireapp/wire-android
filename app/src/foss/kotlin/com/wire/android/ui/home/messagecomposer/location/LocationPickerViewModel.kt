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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor() : ViewModel() {
    var state: LocationPickerState by mutableStateOf(LocationPickerState())
        private set

    fun onPermissionsDialogDiscarded() {
        state = state.copy(showPermissionDeniedDialog = false)
    }

    fun onPermissionsDenied() {
        state = state.copy(showPermissionDeniedDialog = true)
    }

    private fun toStartLoadingLocationState() {
        state = state.copy(isLocationLoading = true, geoLocatedAddress = null)
    }

    private fun toLocationLoadedState(geoLocatedAddress: GeoLocatedAddress) {
        state = state.copy(isLocationLoading = false, geoLocatedAddress = geoLocatedAddress)
    }

    fun getCurrentLocation(context: Context) {
        toStartLoadingLocationState()
        getLocationWithoutGms(context)
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWithoutGms(context: Context) = viewModelScope.launch {
        if (isLocationServicesEnabled(context)) {
            appLogger.d("Getting location without GMS")
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
}
