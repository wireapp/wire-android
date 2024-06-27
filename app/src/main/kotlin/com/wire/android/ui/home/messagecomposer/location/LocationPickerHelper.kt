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
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.VisibleForTesting
import androidx.core.location.LocationManagerCompat
import com.wire.android.AppJsonStyledLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.ui.home.appLock.CurrentTimestampProvider
import com.wire.kalium.logger.KaliumLogLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SuppressLint("MissingPermission")
@Singleton
class LocationPickerHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val currentTimestampProvider: CurrentTimestampProvider,
    private val geocoder: Geocoder,
    private val parameters: LocationPickerParameters,
) {

    @VisibleForTesting
    fun getLocationWithoutGms(onSuccess: (GeoLocatedAddress) -> Unit, onError: () -> Unit) {
        if (isLocationServicesEnabled()) {
            AppJsonStyledLogger.log(
                level = KaliumLogLevel.INFO,
                leadingMessage = "GetLocation",
                jsonStringKeyValues = mapOf("isUsingGms" to false)
            )
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER).let { lastLocation ->
                if (
                    lastLocation != null
                        && currentTimestampProvider() - lastLocation.time <= parameters.lastLocationTimeLimit.inWholeMilliseconds
                ) {
                    onSuccess(lastLocation.toGeoLocatedAddress()) // use last known location if present and not older than given limit
                } else {
                    locationManager.requestCurrentLocationWithoutGms(onSuccess, onError)
                }
            }
        } else {
            AppJsonStyledLogger.log(
                level = KaliumLogLevel.WARN,
                leadingMessage = "GetLocation",
                jsonStringKeyValues = mapOf(
                    "isUsingGms" to false,
                    "error" to "Location services are not enabled"
                )
            )
            onError()
        }
    }

    private fun LocationManager.requestCurrentLocationWithoutGms(onSuccess: (GeoLocatedAddress) -> Unit, onError: () -> Unit) {
        val cancellationSignal = CancellationSignal()
        val timeoutJob = scope.launch(start = CoroutineStart.LAZY) {
            delay(parameters.requestLocationTimeout)
            cancellationSignal.cancel()
            onError()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val executor = context.mainExecutor
            val consumer: Consumer<Location?> = Consumer { location ->
                timeoutJob.cancel()
                if (location != null) {
                    onSuccess(location.toGeoLocatedAddress())
                } else {
                    onError()
                }
            }
            this.getCurrentLocation(LocationManager.FUSED_PROVIDER, cancellationSignal, executor, consumer)
        } else {
            val listener = LocationListener { location ->
                timeoutJob.cancel()
                onSuccess(location.toGeoLocatedAddress())
            }
            cancellationSignal.setOnCancelListener {
                this.removeUpdates(listener)
            }
            this.requestSingleUpdate(LocationManager.FUSED_PROVIDER, listener, null)
        }
        timeoutJob.start()
    }

    internal fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun Location.toGeoLocatedAddress(): GeoLocatedAddress =
        geocoder.getFromLocation(latitude, longitude, 1).orEmpty().let { addressList ->
            GeoLocatedAddress(addressList.firstOrNull(), this)
        }
}

data class LocationPickerParameters(
    val lastLocationTimeLimit: Duration = 1.minutes,
    val requestLocationTimeout: Duration = 10.seconds,
)
