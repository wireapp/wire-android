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

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import javax.inject.Inject

class OtherAccountMapper @Inject constructor(
    private val wireSessionImageLoader: WireSessionImageLoader
) {
    fun toOtherAccount(selfUser: SelfUser, team: Team?): OtherAccount = OtherAccount(
        id = selfUser.id,
        fullName = selfUser.name ?: "",
        avatarData = selfUser.avatar(wireSessionImageLoader, selfUser.connectionStatus),
        teamName = team?.name
    )
}
