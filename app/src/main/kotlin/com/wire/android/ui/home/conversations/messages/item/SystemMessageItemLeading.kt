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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage

@Composable
fun SystemMessageItemLeading(messageContent: SystemMessage, modifier: Modifier = Modifier) {
    if (messageContent.iconResId != null) {
        Image(
            painter = painterResource(id = messageContent.iconResId),
            contentDescription = null,
            colorFilter = getColorFilter(messageContent),
            modifier = modifier.size(
                if (messageContent.isSmallIcon) dimensions().systemMessageIconSize
                else dimensions().systemMessageIconLargeSize
            ),
            contentScale = ContentScale.Crop
        )
    }
}

@Suppress("ComplexMethod")
@Composable
private fun getColorFilter(message: SystemMessage): ColorFilter? {
    return when (message) {
        is SystemMessage.MissedCall.OtherCalled -> null
        is SystemMessage.MissedCall.YouCalled -> null
        is SystemMessage.ConversationDegraded -> null
        is SystemMessage.ConversationVerified -> null
        is SystemMessage.Knock -> ColorFilter.tint(colorsScheme().primary)
        is SystemMessage.LegalHold,
        is SystemMessage.MemberFailedToAdd -> ColorFilter.tint(colorsScheme().error)

        is SystemMessage.MemberAdded,
        is SystemMessage.MemberJoined,
        is SystemMessage.MemberLeft,
        is SystemMessage.MemberRemoved,
        is SystemMessage.CryptoSessionReset,
        is SystemMessage.RenamedConversation,
        is SystemMessage.TeamMemberRemoved_Legacy,
        is SystemMessage.ConversationReceiptModeChanged,
        is SystemMessage.HistoryLost,
        is SystemMessage.HistoryLostProtocolChanged,
        is SystemMessage.NewConversationReceiptMode,
        is SystemMessage.ConversationProtocolChanged,
        is SystemMessage.ConversationProtocolChangedWithCallOngoing,
        is SystemMessage.ConversationMessageTimerActivated,
        is SystemMessage.ConversationMessageCreated,
        is SystemMessage.ConversationStartedWithMembers,
        is SystemMessage.ConversationMessageTimerDeactivated,
        is SystemMessage.FederationMemberRemoved,
        is SystemMessage.FederationStopped,
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning,
        is SystemMessage.TeamMemberRemoved,
        is SystemMessage.MLSWrongEpochWarning -> ColorFilter.tint(colorsScheme().onBackground)
    }
}
