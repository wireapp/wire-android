/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.registration.selector

import android.os.Parcel
import android.os.Parcelable
import com.wire.kalium.logic.configuration.server.ServerConfig
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<ServerConfig.Links?, ServerConfigLinksParceler>()
data class CreateAccountSelectorNavArgs(
    val customServerConfig: ServerConfig.Links? = null,
    val email: String? = null
) : Parcelable

object ServerConfigLinksParceler : Parceler<ServerConfig.Links?> {
    override fun create(parcel: Parcel) = parcel.readBundle()?.let {
        Bundlizer.unbundle(ServerConfig.Links.serializer(), it)
    }

    override fun ServerConfig.Links?.write(parcel: Parcel, flags: Int) {
        if (this != null) {
            parcel.writeBundle(Bundlizer.bundle(ServerConfig.Links.serializer(), this))
        }
    }
}
