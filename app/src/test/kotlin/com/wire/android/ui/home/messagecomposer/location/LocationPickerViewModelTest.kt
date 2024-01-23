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
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
            .withGetGeoLocation()
            .arrange()

        // when
        viewModel.getCurrentLocation()

        // then
        assertEquals(false, viewModel.state.showLocationSharingError)
        assertEquals(true, viewModel.state.geoLocatedAddress != null)
    }


    private class Arrangement {

        val locationPickerHelper = mockk<LocationPickerHelper>(relaxed = true)

        //        val getLocationWithGms = mockk<(GeoLocatedAddress, Unit) -> Unit>(relaxed = true, relaxUnitFun = true)
        val getLocationWithGms = mockkStatic(LocationPickerHelper::getLocationWithGms)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)

        }

        fun withLocationServicesState(enabled: Boolean = true) = apply {
            every { locationPickerHelper.isLocationServicesEnabled() } returns enabled
        }

        fun withIsGoogleServicesAvailable(enabled: Boolean = true) = apply {
            every { locationPickerHelper.isGoogleServicesAvailable() } returns enabled
        }

        fun withGetGeoLocation() = apply {
            coEvery { getLocationWithGms() } returns { Unit }
            coEvery { locationPickerHelper.getLocationWithGms(any(), any()) } coAnswers {
                firstArg<(GeoLocatedAddress) -> Unit>().invoke(GeoLocatedAddress(null, Location("")))
            }
        }

        fun arrange() = this to LocationPickerViewModel(locationPickerHelper)

    }
}
