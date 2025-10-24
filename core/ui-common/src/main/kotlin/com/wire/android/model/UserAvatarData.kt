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

package com.wire.android.model

import androidx.compose.runtime.Stable
import com.wire.android.ui.common.R
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class UserAvatarData(
    val asset: ImageAsset.UserAvatarAsset? = null,
    val availabilityStatus: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val connectionState: ConnectionState? = null,
    val membership: Membership = Membership.None,
    val nameBasedAvatar: NameBasedAvatar? = null
) {

    fun shouldPreferNameBasedAvatar(): Boolean {
        return asset == null && nameBasedAvatar != null &&
                nameBasedAvatar.initials.isEmpty().not() && membership != Membership.Service
    }

    fun getAvailabilityStatusDescriptionId(): Int? = when (availabilityStatus) {
        UserAvailabilityStatus.NONE -> null
        UserAvailabilityStatus.AVAILABLE -> R.string.user_profile_status_available
        UserAvailabilityStatus.AWAY -> R.string.user_profile_status_away
        UserAvailabilityStatus.BUSY -> R.string.user_profile_status_busy
    }
}

/**
 * Holder that can be used to generate an avatar based on the user's full name initials and accent color.
 */
@Serializable
data class NameBasedAvatar(val fullName: String?, val accentColor: Int) {
    val initials: String
        get() {
            if (fullName.isNullOrEmpty()) return String.EMPTY
            val names = fullName.split(EMPTY_REGEX).filter { it.isNotEmpty() }.map { it.uppercase() }
            return when {
                names.size > 1 -> {
                    val initials = names.map { it.first() }
                    initials.first().toString() + initials.last()
                }

                else -> names.first().take(2)
            }
        }
}

val EMPTY_REGEX = "\\s".toRegex()
