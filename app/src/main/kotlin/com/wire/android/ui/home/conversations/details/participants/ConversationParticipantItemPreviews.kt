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

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationParticipantItem() {
    WireTheme {
        ConversationParticipantItem(
            UIParticipant(
                UserId("0", ""),
                "name",
                "handle",
                false,
                false,
                UserAvatarData(),
                Membership.Guest,
                isMLSVerified = true,
                isProteusVerified = true,
                isUnderLegalHold = true,
                supportedProtocolList = listOf(SupportedProtocol.PROTEUS, SupportedProtocol.MLS)
            ),
            clickable = Clickable(enabled = true) {},
            developerFeaturesEnabled = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationTemporaryParticipantItem() {
    WireTheme {
        ConversationParticipantItem(
            UIParticipant(
                UserId("0", ""),
                "name",
                "handle",
                false,
                false,
                UserAvatarData(),
                Membership.Guest,
                isMLSVerified = true,
                isProteusVerified = true,
                isUnderLegalHold = true,
                supportedProtocolList = listOf(SupportedProtocol.PROTEUS, SupportedProtocol.MLS),
                expiresAt = Clock.System.now().plus(23, DateTimeUnit.HOUR)
            ),
            clickable = Clickable(enabled = true) {},
            developerFeaturesEnabled = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationReadReceiptItem() {
    WireTheme {
        ConversationParticipantItem(
            UIParticipant(
                UserId("0", ""),
                "name",
                "handle",
                false,
                false,
                UserAvatarData(),
                Membership.Guest,
                isMLSVerified = true,
                isProteusVerified = true,
                isUnderLegalHold = true,
                supportedProtocolList = listOf(SupportedProtocol.PROTEUS, SupportedProtocol.MLS),
                readReceiptDate = Clock.System.now()
            ),
            clickable = Clickable(enabled = true) {},
            developerFeaturesEnabled = true,
        )
    }
}
