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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.ProtocolLabel
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType.WithIndicators
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.UsernameMapper.fromExpirationToHandle
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.uiReadReceiptDateTime
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

@Composable
fun ConversationParticipantItem(
    uiParticipant: UIParticipant,
    clickable: Clickable,
    modifier: Modifier = Modifier,
    searchQuery: String = String.EMPTY,
    showRightArrow: Boolean = true
) {
    RowItemTemplate(
        modifier = modifier,
        leadingIcon = {
            UserProfileAvatar(
                avatarData = uiParticipant.avatarData,
                modifier = Modifier.padding(
                    start = dimensions().spacing8x
                ),
                contentDescription = null,
                type = uiParticipant.expiresAt?.let { WithIndicators.TemporaryUser(it) } ?: WithIndicators.RegularUser()
            )
        },
        titleStartPadding = dimensions().spacing0x,
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
                if (BuildConfig.DEVELOPER_FEATURES_ENABLED) {
                    uiParticipant.supportedProtocolList.map {
                        ProtocolLabel(
                            protocolName = it.name,
                            Modifier.padding(start = dimensions().spacing4x)
                        )
                    }
                }
                if (uiParticipant.isUnderLegalHold) {
                    LegalHoldIndicator(modifier = Modifier.padding(start = dimensions().spacing6x))
                }
            }
        },
        subtitle = {
            val userName = processUsername(uiParticipant)
            // Availability status should be called after username by TalkBack
            val subtitleModifier = uiParticipant.avatarData.getAvailabilityStatusDescriptionId()?.let {
                val contentDescription = stringResource(it)
                Modifier.semantics { this.contentDescription = "$userName, $contentDescription" }
            } ?: Modifier

            HighlightSubtitle(
                subTitle = userName,
                searchQuery = searchQuery,
                prefix = processUsernamePrefix(uiParticipant),
                modifier = subtitleModifier
            )
        },
        actions = {
            if (showRightArrow) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(end = MaterialTheme.wireDimensions.spacing8x)
                ) {
                    ArrowRightIcon(
                        modifier = Modifier.align(Alignment.TopEnd),
                        contentDescription = R.string.content_description_empty
                    )
                }
            }
        },
        clickable = clickable
    )
}

@Composable
private fun processUsernamePrefix(uiParticipant: UIParticipant) = when {
    uiParticipant.readReceiptDate != null || uiParticipant.expiresAt != null -> ""
    else -> "@"
}

@Composable
private fun processUsername(uiParticipant: UIParticipant) = when {
    uiParticipant.unavailable -> uiParticipant.id.domain
    uiParticipant.readReceiptDate != null -> uiParticipant.readReceiptDate.uiReadReceiptDateTime()
    uiParticipant.expiresAt != null -> {
        val expiresAtString = fromExpirationToHandle(uiParticipant.expiresAt)
        stringResource(R.string.temporary_user_label, expiresAtString)
    }

    else -> uiParticipant.handle
}

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
            clickable = Clickable(enabled = true) {}
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
            clickable = Clickable(enabled = true) {}
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
            clickable = Clickable(enabled = true) {}
        )
    }
}
