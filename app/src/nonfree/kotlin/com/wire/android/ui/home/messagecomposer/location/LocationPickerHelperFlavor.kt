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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wire.android.AppJsonStyledLogger
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.kalium.logger.KaliumLogLevel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationPickerHelperFlavor @Inject constructor(
    private val context: Context,
    private val geocoderHelper: GeocoderHelper,
    private val locationPickerHelper: LocationPickerHelper,
) {
    suspend fun getLocation(onSuccess: (GeoLocatedAddress) -> Unit, onError: () -> Unit) {
        if (context.isGoogleServicesAvailable()) {
            getLocationWithGms(
                onSuccess = onSuccess,
                onError = onError
            )
        } else {
            locationPickerHelper.getLocationWithoutGms(
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    /**
     * Choosing the best location estimate by docs.
     * https://developer.android.com/develop/sensors-and-location/location/retrieve-current#BestEstimate
     */
    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getLocationWithGms(onSuccess: (GeoLocatedAddress) -> Unit, onError: () -> Unit) {
        if (locationPickerHelper.isLocationServicesEnabled()) {
            AppJsonStyledLogger.log(
                level = KaliumLogLevel.INFO,
                leadingMessage = "GetLocation",
                jsonStringKeyValues = mapOf("isUsingGms" to true)
            )
            try {
                val locationProvider = LocationServices.getFusedLocationProviderClient(context)
                val currentLocation =
                    locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                onSuccess(geocoderHelper.getGeoLocatedAddress(currentLocation))
            } catch (e: Exception) {
                AppJsonStyledLogger.log(
                    level = KaliumLogLevel.WARN,
                    leadingMessage = "GetLocation",
                    jsonStringKeyValues = mapOf(
                        "isUsingGms" to true,
                        "error" to "Location services are not available"
                    )
                )
                onError()
            }
        } else {
            AppJsonStyledLogger.log(
                level = KaliumLogLevel.WARN,
                leadingMessage = "GetLocation",
                jsonStringKeyValues = mapOf(
                    "isUsingGms" to true,
                    "error" to "Location services are not enabled"
                )
            )
            onError()
        }
    }
}
