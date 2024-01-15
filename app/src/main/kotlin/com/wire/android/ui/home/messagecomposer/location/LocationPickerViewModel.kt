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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource
import com.wire.android.appLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor() : ViewModel() {
    var state: LocationPickerState by mutableStateOf(LocationPickerState())
        private set

    init {
        appLogger.d("LocationPickerViewModel init")
    }

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

    /**
     * Choosing the best location estimate by docs.
     * https://developer.android.com/develop/sensors-and-location/location/retrieve-current#BestEstimate
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            toStartLoadingLocationState()
            val locationProvider = LocationServices.getFusedLocationProviderClient(context)
            val currentLocation = locationProvider.getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            val address = Geocoder(context).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).orEmpty()
            toLocationLoadedState(GeoLocatedAddress(address.firstOrNull(), currentLocation))
        }
    }
}
