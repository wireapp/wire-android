/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.nomaddevice.dao

import com.wire.android.BuildConfig
import com.wire.kalium.nomaddevice.dao.NomadDeviceMetadataDAO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NomadDeviceDaoRepository @Inject constructor(
    private val nomadDeviceMetadataDAO: NomadDeviceMetadataDAO
) {
    suspend fun warmUp() {
        if (nomadDeviceMetadataDAO.getDeviceIdentifier() == null) {
            nomadDeviceMetadataDAO.putDeviceIdentifier(BuildConfig.FLAVOR)
        }
    }
}
