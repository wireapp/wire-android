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
package com.wire.android.ui.authentication.create.common

import android.os.Parcelable
import com.wire.android.ui.registration.selector.ServerConfigLinksParceler
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Deprecated("This nav arg belongs to the old registration flow, please use the new one [CreateAccountDataNavArgs]")
@Parcelize
@TypeParceler<ServerConfig.Links?, ServerConfigLinksParceler>()
data class CreateAccountNavArgs(
    val flowType: CreateAccountFlowType,
    val userRegistrationInfo: UserRegistrationInfo = UserRegistrationInfo(),
    val customServerConfig: ServerConfig.Links? = null,
) : Parcelable

@Parcelize
@TypeParceler<ServerConfig.Links?, ServerConfigLinksParceler>()
data class CreateAccountDataNavArgs(
    val userRegistrationInfo: UserRegistrationInfo = UserRegistrationInfo(),
    val customServerConfig: ServerConfig.Links? = null,
    val canTrackRegistration: Boolean = false
) : Parcelable

@Parcelize
data class UserRegistrationInfo(
    val email: String = String.EMPTY,
    val name: String = String.EMPTY,
    val firstName: String = String.EMPTY,
    val lastName: String = String.EMPTY,
    val password: String = String.EMPTY,
    val teamName: String = String.EMPTY,
    val teamIcon: String = "default"
) : Parcelable
