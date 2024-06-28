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

import android.location.Geocoder
import android.location.Location
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocoderHelper @Inject constructor(private val geocoder: Geocoder) {

    @Suppress("TooGenericExceptionCaught")
    fun getGeoLocatedAddress(location: Location): GeoLocatedAddress =
        try {
            geocoder.getFromLocation(location.latitude, location.longitude, 1).orEmpty()
        } catch (e: Exception) {
            emptyList()
        }.let { addressList ->
            GeoLocatedAddress(addressList.firstOrNull(), location)
        }
}
