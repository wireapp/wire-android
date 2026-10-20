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

package com.wire.android.ui.home.conversations.details.participants.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import kotlin.time.Instant

data class UIParticipant(
    val id: UserId,
    val name: String,
    val handle: String,
    val isSelf: Boolean,
    val isService: Boolean = false,
    val avatarData: UserAvatarData = UserAvatarData(),
    val membership: Membership = Membership.None,
    val connectionState: ConnectionState? = null,
    val unavailable: Boolean = false,
    val isDeleted: Boolean = false,
    val readReceiptDate: Instant? = null,
    val botService: BotService? = null,
    val isDefederated: Boolean = false,
    val isProteusVerified: Boolean = false,
    val isMLSVerified: Boolean = false,
    val supportedProtocolList: List<SupportedProtocol> = listOf(),
    val isUnderLegalHold: Boolean = false,
    val expiresAt: Instant? = null
)
