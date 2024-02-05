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

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class LocationPickerHelperTest {

    @Test
    fun `given user has device location disabled, when sharing location, then error lambda is called`() = runTest {
        // given
        val (arrangement, locationHelper) = Arrangement()
            .withLocationEnabled(false)
            .arrange()

        // when - then
        locationHelper.getLocation(
            onSuccess = {

            },
            onError = { assertTrue(true) }
        )
    }


    @Test
    fun `given user has device location enabled, when sharing location, then on success lambda is called`() = runTest {
        // given
        val (arrangement, locationHelper) = Arrangement()
            .withLocationEnabled(true)
            .arrange()

        // when - then
        locationHelper.getLocation(
            onSuccess = {
                assertTrue(true)
            },
            onError = {
                assertTrue(false) // this should not be called, so it will fail the test otherwise.
            }
        )
    }

    private class Arrangement {
        val context: Context = ApplicationProvider.getApplicationContext()
        val locationManager: LocationManager = context.getSystemService(Application.LOCATION_SERVICE) as LocationManager

        init {
            shadowOf(locationManager).apply {
                setProviderEnabled(LocationManager.GPS_PROVIDER, true)
                setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            }
        }

        fun withLocationEnabled(enabled: Boolean) = apply {
            locationManager.apply {
                shadowOf(this).apply {
                    setLocationEnabled(enabled)
                }
            }
        }


        fun arrange() = this to LocationPickerHelper(context)
    }

}
