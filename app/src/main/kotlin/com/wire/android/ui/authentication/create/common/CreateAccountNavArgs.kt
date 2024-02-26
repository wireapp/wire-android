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
import com.wire.android.util.EMPTY
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreateAccountNavArgs(
    val flowType: CreateAccountFlowType,
    val userRegistrationInfo: UserRegistrationInfo = UserRegistrationInfo()
) : Parcelable

@Parcelize
data class UserRegistrationInfo(
    val email: String = String.EMPTY,
    val firstName: String = String.EMPTY,
    val lastName: String = String.EMPTY,
    val password: String = String.EMPTY,
    val teamName: String = String.EMPTY,
    val teamIcon: String = "default"
) : Parcelable
