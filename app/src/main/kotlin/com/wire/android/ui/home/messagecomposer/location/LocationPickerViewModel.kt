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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor() : ViewModel() {
    var state: LocationPickerState by mutableStateOf(LocationPickerState())
        private set

    fun setPermissionsAllowed(isAllowed: Boolean) {
        state = state.copy(isPermissionsAllowed = isAllowed)
    }

    fun onLocationPicked(geoLocatedAddress: GeoLocatedAddress) {
        state = state.copy(geoLocatedAddress = geoLocatedAddress)
    }

    /**
     * Choosing the best location estimate by docs.
     * https://developer.android.com/develop/sensors-and-location/location/retrieve-current#BestEstimate
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onCurrentLocationPicked: (GeoLocatedAddress) -> Unit, context: Context) {
        val locationProvider = LocationServices.getFusedLocationProviderClient(context)
        viewModelScope.launch {
            val currentLocation = locationProvider.getCurrentLocation(PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            val address = Geocoder(context).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).orEmpty()
            onCurrentLocationPicked(GeoLocatedAddress(address.firstOrNull(), currentLocation))
        }
    }

}
