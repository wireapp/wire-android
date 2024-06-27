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

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.util.extension.isGoogleServicesAvailable
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class LocationPickerHelperFlavorTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `given GMS not available, when getting location, then execute getLocationWithoutGms`() =
        runTest(dispatcher) {
            // given
            val (arrangement, locationPickerHelperFlavor) = Arrangement()
                .withIsGoogleServicesAvailable(false)
                .arrange()

            // when
            locationPickerHelperFlavor.getLocation(onSuccess = arrangement.onSuccess, onError = arrangement.onError)

            // then
            coVerify(exactly = 1) {
                arrangement.locationPickerHelper.getLocationWithoutGms(any(), any())
            }
        }

    @Test
    fun `given GMS available and location service disabled, when getting location, then execute onError`() =
        runTest(dispatcher) {
            // given
            val (arrangement, locationPickerHelperFlavor) = Arrangement()
                .withIsGoogleServicesAvailable(true)
                .withIsLocationServiceEnabled(false)
                .arrange()

            // when
            locationPickerHelperFlavor.getLocation(onSuccess = arrangement.onSuccess, onError = arrangement.onError)

            // then
            coVerify(exactly = 0) {
                arrangement.onSuccess(any())
            }
            coVerify(exactly = 1) {
                arrangement.onError()
            }
        }

    @Test
    fun `given GMS available and location service enabled, when getting location, then execute onSuccess with location`() =
        runTest(dispatcher) {
            // given
            val location = mockLocation(latitude = 1.0, longitude = 1.0)
            val address = mockAddress(addressFirstLine = "address")
            val (arrangement, locationPickerHelperFlavor) = Arrangement()
                .withIsGoogleServicesAvailable(true)
                .withIsLocationServiceEnabled(true)
                .withGetCurrentLocation(location)
                .withGeocoderGetFromLocation(1.0, 1.0, address)
                .arrange()

            // when
            locationPickerHelperFlavor.getLocation(onSuccess = arrangement.onSuccess, onError = arrangement.onError)

            // then
            coVerify(exactly = 1) {
                arrangement.onSuccess(match { it.location == location && it.address == address })
            }
            coVerify(exactly = 0) {
                arrangement.onError()
            }
        }

    private fun MockKMatcherScope.match(expected: GeoLocatedAddress): GeoLocatedAddress =
        match {
            it.location.latitude == expected.location.latitude &&
                    it.location.longitude == expected.location.longitude &&
                    it.address?.getAddressLine(0) == expected.address?.getAddressLine(0)
        }

    inner class Arrangement {

        @MockK
        private lateinit var context: Context

        @MockK
        private lateinit var locationManager: LocationManager

        @MockK
        private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

        @MockK
        private lateinit var geocoder: Geocoder

        @MockK
        lateinit var locationPickerHelper: LocationPickerHelper

        val onSuccess: (GeoLocatedAddress) -> Unit = mockk()
        val onError: () -> Unit = mockk()

        private val locationPickerHelperFlavor by lazy {
            LocationPickerHelperFlavor(
                context = context,
                geocoder = geocoder,
                locationPickerHelper = locationPickerHelper,
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(LocationServices::getFusedLocationProviderClient)
            coEvery { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationProviderClient
            coEvery { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
            coEvery { onSuccess(any()) } returns Unit
            coEvery { onError() } returns Unit
            coEvery { locationPickerHelper.getLocationWithoutGms(any(), any()) } returns Unit
        }

        fun withIsGoogleServicesAvailable(isAvailable: Boolean) = apply {
            mockkStatic("com.wire.android.util.extension.GoogleServicesKt")
            coEvery { context.isGoogleServicesAvailable() } returns isAvailable
        }

        fun withIsLocationServiceEnabled(isEnabled: Boolean) = apply {
            coEvery { locationPickerHelper.isLocationServicesEnabled() } returns isEnabled
        }

        fun withGetCurrentLocation(location: Location) = apply {
            val task: Task<Location> = mockk()
            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            coEvery { task.await() } returns location
            mockkConstructor(CancellationTokenSource::class)
            coEvery { anyConstructed<CancellationTokenSource>().token } returns mockk()
            coEvery { fusedLocationProviderClient.getCurrentLocation(any<Int>(), any<CancellationToken>()) } returns task
        }

        fun withGeocoderGetFromLocation(latitude: Double, longitude: Double, result: Address) = apply {
            coEvery { geocoder.getFromLocation(latitude, longitude, 1) } returns listOf(result)
        }

        fun arrange() = this to locationPickerHelperFlavor
    }

    fun mockLocation(latitude: Double, longitude: Double) = mockk<Location>().let {
        coEvery { it.latitude } returns latitude
        coEvery { it.longitude } returns longitude
        it
    }

    fun mockAddress(addressFirstLine: String) = mockk<Address>().also {
        coEvery { it.getAddressLine(0) } returns addressFirstLine
    }
}
