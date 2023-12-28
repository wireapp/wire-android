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

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.ProtocolLabel
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.uiReadReceiptDateTime
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationParticipantItem(
    uiParticipant: UIParticipant,
    searchQuery: String = String.EMPTY,
    clickable: Clickable,
    showRightArrow: Boolean = true
) {
    RowItemTemplate(
        leadingIcon = {
            UserProfileAvatar(
                uiParticipant.avatarData,
                modifier = Modifier.padding(
                    start = dimensions().spacing8x
                )
            )
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = if (uiParticipant.unavailable) stringResource(R.string.username_unavailable_label) else uiParticipant.name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                if (uiParticipant.isSelf) {
                    Text(
                        text = stringResource(R.string.conversation_participant_you_label),
                        style = MaterialTheme.wireTypography.title02.copy(
                            color = MaterialTheme.wireColorScheme.secondaryText
                        ),
                        modifier = Modifier
                            .padding(
                                start = dimensions().spacing4x,
                                end = dimensions().spacing4x
                            )
                    )
                }
                UserBadge(
                    membership = uiParticipant.membership,
                    connectionState = uiParticipant.connectionState,
                    startPadding = dimensions().spacing6x,
                    isDeleted = uiParticipant.isDeleted
                )

                if (uiParticipant.isMLSVerified) MLSVerifiedIcon()
                if (uiParticipant.isProteusVerified) ProteusVerifiedIcon()
                if (BuildConfig.MLS_SUPPORT_ENABLED && BuildConfig.DEVELOPER_FEATURES_ENABLED) {
                    uiParticipant.supportedProtocolList.map {
                        ProtocolLabel(
                            protocolName = it.name,
                            Modifier.padding(start = dimensions().spacing4x)
                        )
                    }
                }
            }
        },
        subtitle = {
            HighlightSubtitle(
                subTitle = if (uiParticipant.unavailable) {
                    uiParticipant.id.domain
                } else uiParticipant.readReceiptDate?.let {
                    it.uiReadReceiptDateTime()
                } ?: uiParticipant.handle,
                searchQuery = searchQuery,
                suffix = uiParticipant.readReceiptDate?.let { "" } ?: "@"
            )
        },
        actions = {
            if (showRightArrow) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(end = MaterialTheme.wireDimensions.spacing8x)
                ) {
                    ArrowRightIcon(Modifier.align(Alignment.TopEnd))
                }
            }
        },
        clickable = clickable
    )
}

@Preview
@Composable
fun PreviewGroupConversationParticipantItem() {
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
            supportedProtocolList = listOf(SupportedProtocol.PROTEUS, SupportedProtocol.MLS)),
        clickable = Clickable(enabled = true) {}
    )
}
