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
import android.location.Location
import com.wire.android.util.orDefault

data class GeoLocatedAddress(
    val address: Address?,
    val location: Location
) {

    fun getFormattedAddress(): String {
        return address?.let {
            "${address.featureName.orDefault(address.adminArea.orEmpty())}, ${address.postalCode.orDefault(address.adminArea.orEmpty())}, ${address.countryCode}"
        } ?: "${location.latitude}, ${location.longitude}"
    }

}
