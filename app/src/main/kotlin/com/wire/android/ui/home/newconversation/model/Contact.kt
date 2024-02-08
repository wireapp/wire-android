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

package com.wire.android.ui.home.newconversation.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.ConnectionState

data class Contact(
    val id: String,
    val domain: String,
    val name: String,
    val avatarData: UserAvatarData = UserAvatarData(),
    val label: String = "",
    val connectionState: ConnectionState,
    val membership: Membership
) {
    fun isMetadataEmpty(): Boolean {
        return name.isEmpty()
    }

    fun fullyQualifiedHandle() = "$label@$domain"
}
