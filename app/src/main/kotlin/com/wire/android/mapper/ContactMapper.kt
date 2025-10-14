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

package com.wire.android.mapper

import com.wire.android.model.ImageAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.userprofile.common.UsernameMapper
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.publicuser.model.UserSearchDetails
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType
import javax.inject.Inject

class ContactMapper
@Inject constructor(
    private val userTypeMapper: UserTypeMapper,
) {

    fun fromOtherUser(otherUser: OtherUser): Contact {
        with(otherUser) {
            return Contact(
                id = id.value,
                domain = id.domain,
                name = name.orEmpty(),
                handle = handle.orEmpty(),
                label = UsernameMapper.fromOtherUser(otherUser),
                avatarData = UserAvatarData(
                    asset = previewPicture?.let { ImageAsset.UserAvatarAsset(it) },
                    connectionState = connectionStatus,
                    nameBasedAvatar = NameBasedAvatar(fullName = name, accentColor = otherUser.accentId)
                ),
                membership = userTypeMapper.toMembership(userType),
                connectionState = otherUser.connectionStatus
            )
        }
    }

    fun fromService(service: ServiceDetails): Contact {
        with(service) {
            return Contact(
                id = id.id,
                domain = id.provider,
                name = name,
                handle = String.EMPTY,
                label = String.EMPTY,
                avatarData = UserAvatarData(
                    asset = previewAssetId?.let { ImageAsset.UserAvatarAsset(it) },
                    membership = Membership.Service
                ),
                membership = Membership.Service,
                connectionState = ConnectionState.ACCEPTED
            )
        }
    }

    fun fromSearchUserResult(user: UserSearchDetails): Contact {
        with(user) {
            return Contact(
                id = id.value,
                domain = id.domain,
                name = name.orEmpty(),
                handle = handle.orEmpty(),
                label = mapUserHandle(user),
                avatarData = UserAvatarData(
                    asset = previewAssetId?.let { ImageAsset.UserAvatarAsset(it) },
                    nameBasedAvatar = NameBasedAvatar(fullName = name, accentColor = -1)
                ),
                membership = userTypeMapper.toMembership(type),
                connectionState = connectionStatus
            )
        }
    }

    /**
     * Adds the fully qualified handle to the contact label in case of federated users.
     */
    private fun mapUserHandle(user: UserSearchDetails): String {
        return with(user.type) {
            when (type) {
                UserType.FEDERATED -> "${user.handle}@${user.id.domain}"
                else -> user.handle ?: String.EMPTY
            }
        }
    }
}
