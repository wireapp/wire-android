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

import android.location.Location
import com.wire.android.config.CoroutineTestExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class LocationPickerViewModelTest {

    @Test
    fun `given user has device location disabled, when sharing location, then an error message will be shown`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withLocationServicesState(false)
            .withIsGoogleServicesAvailable(true)
            .withGetGeoLocationErrorFrom()
            .arrange()

        // when
        viewModel.getCurrentLocation()

        // then
        assertEquals(true, viewModel.state.showLocationSharingError)
    }

    @Test
    fun `given user has device location enabled, when sharing location, then should load the location`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withLocationServicesState(true)
            .withIsGoogleServicesAvailable(true)
            .withGetGeoLocationSuccessFrom()
            .arrange()

        // when
        viewModel.getCurrentLocation()

        // then
        assertEquals(false, viewModel.state.showLocationSharingError)
        assertEquals(true, viewModel.state.geoLocatedAddress != null)
        coVerify(exactly = 1) { arrangement.locationPickerHelper.getLocationWithGms(any(), any()) }
    }

    @Test
    fun `given user has device location enabled and no google services, when sharing location, then should load the location without gms`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withLocationServicesState(true)
                .withIsGoogleServicesAvailable(false)
                .withGetGeoLocationSuccessFrom(false)
                .arrange()

            // when
            viewModel.getCurrentLocation()

            // then
            assertEquals(false, viewModel.state.showLocationSharingError)
            assertEquals(true, viewModel.state.geoLocatedAddress != null)
            coVerify(exactly = 1) { arrangement.locationPickerHelper.getLocationWithoutGms(any(), any()) }
        }

    private class Arrangement {

        val locationPickerHelper = mockk<LocationPickerHelper>()

        fun withLocationServicesState(enabled: Boolean = true) = apply {
            every { locationPickerHelper.isLocationServicesEnabled() } returns enabled
        }

        fun withIsGoogleServicesAvailable(enabled: Boolean = true) = apply {
            every { locationPickerHelper.isGoogleServicesAvailable() } returns enabled
        }

        fun withGetGeoLocationSuccessFrom(fromGms: Boolean = true) = apply {
            if (fromGms) {
                coEvery {
                    locationPickerHelper.getLocationWithGms(
                        capture(onEngineStartSuccess),
                        capture(onEngineStartFailure)
                    )
                } coAnswers {
                    firstArg<PickedGeoLocation>().invoke(successResponse)
                }
            } else {
                coEvery {
                    locationPickerHelper.getLocationWithoutGms(
                        capture(onEngineStartSuccess),
                        capture(onEngineStartFailure)
                    )
                } coAnswers {
                    firstArg<PickedGeoLocation>().invoke(successResponse)
                }
            }

        }

        fun withGetGeoLocationErrorFrom(fromGms: Boolean = true) = apply {
            if (fromGms) {
                coEvery {
                    locationPickerHelper.getLocationWithGms(
                        capture(onEngineStartSuccess),
                        capture(onEngineStartFailure)
                    )
                } coAnswers {
                    secondArg<() -> Unit>().invoke()
                }
            } else {
                coEvery {
                    locationPickerHelper.getLocationWithoutGms(
                        capture(onEngineStartSuccess),
                        capture(onEngineStartFailure)
                    )
                } coAnswers {
                    secondArg<() -> Unit>().invoke()
                }
            }

        }

        fun arrange() = this to LocationPickerViewModel(locationPickerHelper)
    }

    private companion object {
        val onEngineStartSuccess = slot<PickedGeoLocation>()
        val onEngineStartFailure = slot<() -> Unit>()

        val successResponse = GeoLocatedAddress(null, Location("dummy-location"))
    }
}

typealias PickedGeoLocation = (GeoLocatedAddress) -> Unit
