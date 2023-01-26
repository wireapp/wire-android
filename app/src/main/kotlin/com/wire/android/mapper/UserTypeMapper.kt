/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.type.UserType
import javax.inject.Inject

class UserTypeMapper @Inject constructor() {

    fun toMembership(userType: UserType) = when (userType) {
        UserType.GUEST -> Membership.Guest
        UserType.FEDERATED -> Membership.Federated
        UserType.EXTERNAL -> Membership.External
        UserType.INTERNAL -> Membership.Standard
        UserType.NONE -> Membership.None
        UserType.SERVICE -> Membership.Service
        UserType.ADMIN -> Membership.Admin
        UserType.OWNER -> Membership.Owner
    }

}
