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
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class LocationPickerViewModelTest {

    @Test
    fun `given user has device location disabled, when sharing location, then an error message will be shown`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withGetGeoLocationError()
            .arrange()

        // when
        viewModel.getCurrentLocation()

        // then
        assertEquals(true, viewModel.state.showLocationSharingError)
        assertEquals(true, viewModel.state.geoLocatedAddress == null)
    }

    @Test
    fun `given user has device location enabled, when sharing location, then should load the location`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withGetGeoLocationSuccess()
            .arrange()

        // when
        viewModel.getCurrentLocation()

        // then
        assertEquals(false, viewModel.state.showLocationSharingError)
        assertEquals(true, viewModel.state.geoLocatedAddress != null)
        coVerify(exactly = 1) { arrangement.locationPickerHelper.getLocation(any(), any()) }
    }

    private class Arrangement {

        val locationPickerHelper = mockk<LocationPickerHelperFlavor>()

        fun withGetGeoLocationSuccess() = apply {
            coEvery {
                locationPickerHelper.getLocation(
                    capture(onPickedLocationSuccess),
                    capture(onPickedLocationFailure)
                )
            } coAnswers {
                firstArg<PickedGeoLocation>().invoke(successResponse)
            }
        }

        fun withGetGeoLocationError() = apply {
            coEvery {
                locationPickerHelper.getLocation(
                    capture(onPickedLocationSuccess),
                    capture(onPickedLocationFailure)
                )
            } coAnswers {
                secondArg<() -> Unit>().invoke()
            }
        }

        fun arrange() = this to LocationPickerViewModel(locationPickerHelper)
    }

    private companion object {
        val onPickedLocationSuccess = slot<PickedGeoLocation>()
        val onPickedLocationFailure = slot<() -> Unit>()
        val successResponse = GeoLocatedAddress(null, Location("dummy-location"))
    }
}

private typealias PickedGeoLocation = (GeoLocatedAddress) -> Unit
