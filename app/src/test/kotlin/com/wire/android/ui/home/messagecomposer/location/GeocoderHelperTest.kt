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

import android.location.Address
import android.location.Geocoder
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeocoderHelperTest {

    @Test
    fun `given non-null result, when getting geocoder address, then return result with address`() = runTest {
        // given
        val location = mockLocation(latitude = 1.0, longitude = 1.0)
        val address = mockAddress(addressFirstLine = "address")
        val (_, geocoderHelper) = Arrangement()
            .withGetFromLocation(1.0, 1.0, address)
            .arrange()

        // when
        val result = geocoderHelper.getGeoLocatedAddress(location)

        // then
        assertEquals(address, result.address)
    }

    @Test
    fun `given empty result, when getting geocoder address, then return result without address`() = runTest {
        // given
        val location = mockLocation(latitude = 1.0, longitude = 1.0)
        val (_, geocoderHelper) = Arrangement()
            .withGetFromLocation(1.0, 1.0, null)
            .arrange()

        // when
        val result = geocoderHelper.getGeoLocatedAddress(location)

        // then
        assertEquals(null, result.address)
    }

    @Test
    fun `given failure, when getting geocoder address, then return result without address`() = runTest {
        // given
        val location = mockLocation(latitude = 1.0, longitude = 1.0)
        val (_, geocoderHelper) = Arrangement()
            .withGetFromLocationFailure()
            .arrange()

        // when
        val result = geocoderHelper.getGeoLocatedAddress(location)

        // then
        assertEquals(null, result.address)
    }

    inner class Arrangement {

        @MockK
        lateinit var geocoder: Geocoder

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withGetFromLocation(latitude: Double, longitude: Double, result: Address?) = apply {
            coEvery { geocoder.getFromLocation(latitude, longitude, 1) } returns listOfNotNull(result)
        }

        fun withGetFromLocationFailure() = apply {
            coEvery { geocoder.getFromLocation(any(), any(), any()) } throws IOException()
        }

        fun arrange() = this to GeocoderHelper(geocoder)
    }
}
