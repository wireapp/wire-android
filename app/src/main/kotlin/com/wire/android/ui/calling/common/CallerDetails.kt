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

package com.wire.android.ui.calling.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.model.ConversationName
import com.wire.android.ui.common.ConversationVerificationIcons
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.ConversationTypeForCall
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallerDetails(
    conversationId: ConversationId,
    conversationName: ConversationName?,
    accentId: Int,
    isCameraOn: Boolean,
    isCbrEnabled: Boolean,
    avatarAssetId: ImageAsset.UserAvatarAsset?,
    conversationTypeForCall: ConversationTypeForCall,
    membership: Membership,
    groupCallerName: String?,
    protocolInfo: Conversation.ProtocolInfo?,
    mlsVerificationStatus: Conversation.VerificationStatus?,
    proteusVerificationStatus: Conversation.VerificationStatus?,
    onMinimiseScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = dimensions().spacing32x),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            modifier = Modifier
                .padding(top = dimensions().spacing16x, start = dimensions().spacing6x)
                .align(Alignment.Start)
                .rotate(180f),
            onClick = {
                onMinimiseScreen()
            }
        ) {
            Image(
                painter = painterResource(id = com.wire.android.ui.common.R.drawable.ic_collapse),
                contentDescription = stringResource(id = R.string.calling_minimize_view),
            )
        }
        if (isCbrEnabled) {
            Text(
                text = stringResource(id = R.string.calling_constant_bit_rate_indication).uppercase(
                    Locale.getDefault()
                ),
                color = colorsScheme().secondaryText,
                style = MaterialTheme.wireTypography.title03,
            )
        }
        Row(modifier = Modifier.padding(top = dimensions().spacing24x)) {
            Text(
                text = when (conversationName) {
                    is ConversationName.Known -> conversationName.name
                    is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                    else -> ""
                },
                color = colorsScheme().onBackground,
                style = MaterialTheme.wireTypography.title01,
            )

            ConversationVerificationIcons(
                protocolInfo,
                mlsVerificationStatus,
                proteusVerificationStatus
            )
        }

        FlowRow(
            modifier = Modifier.padding(
                top = dimensions().spacing8x,
                start = dimensions().spacing24x,
                end = dimensions().spacing24x
            ),
            horizontalArrangement = Arrangement.Center,
        ) {
            groupCallerName?.let { name ->
                Text(
                    text = name,
                    color = colorsScheme().onBackground,
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val callingLabel =
                if (groupCallerName != null) {
                    if (conversationTypeForCall == ConversationTypeForCall.Conference) {
                        stringResource(R.string.calling_label_incoming_call_someone_calling)
                    } else {
                        stringResource(R.string.calling_label_incoming_call)
                    }
                } else {
                    stringResource(R.string.calling_label_ringing_call)
                }
            Text(
                modifier = Modifier.padding(
                    start = dimensions().spacing2x,
                ),
                text = callingLabel,
                color = colorsScheme().onBackground,
                style = MaterialTheme.wireTypography.body01,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (membership.hasLabel()) {
            VerticalSpace.x16()
            MembershipQualifierLabel(membership)
        }

        SecurityClassificationBannerForConversation(
            conversationId = conversationId,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )

        if (!isCameraOn && conversationTypeForCall == ConversationTypeForCall.OneOnOne) {
            UserProfileAvatar(
                avatarData = UserAvatarData(
                    asset = avatarAssetId,
                    nameBasedAvatar = NameBasedAvatar(
                        (conversationName as? ConversationName.Known)?.name,
                        accentId
                    )
                ),
                size = dimensions().outgoingCallUserAvatarSize,
                modifier = Modifier.padding(top = dimensions().spacing16x)
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCallerDetailsOneOnOneCall() {
    WireTheme {
        CallerDetails(
            conversationId = ConversationId("value", "domain"),
            conversationName = ConversationName.Known("Jon Doe"),
            isCameraOn = false,
            isCbrEnabled = false,
            avatarAssetId = null,
            conversationTypeForCall = ConversationTypeForCall.OneOnOne,
            membership = Membership.Guest,
            groupCallerName = null,
            protocolInfo = null,
            mlsVerificationStatus = null,
            proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
            onMinimiseScreen = { },
            accentId = -1
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCallerDetailsGroupCallWithLongName() {
    WireTheme {
        CallerDetails(
            conversationId = ConversationId("value", "domain"),
            conversationName = ConversationName.Known("Some fake group name"),
            isCameraOn = false,
            isCbrEnabled = false,
            avatarAssetId = null,
            conversationTypeForCall = ConversationTypeForCall.Conference,
            membership = Membership.Guest,
            groupCallerName = "Caller name long name with lots of characters to make it a long name",
            protocolInfo = null,
            mlsVerificationStatus = null,
            proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
            onMinimiseScreen = { },
            accentId = -1
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCallerDetailsGroupCallWithShortName() {
    WireTheme {
        CallerDetails(
            conversationId = ConversationId("value", "domain"),
            conversationName = ConversationName.Known("Some fake group name"),
            isCameraOn = false,
            isCbrEnabled = false,
            avatarAssetId = null,
            conversationTypeForCall = ConversationTypeForCall.Conference,
            membership = Membership.Guest,
            groupCallerName = "Caller name",
            protocolInfo = null,
            mlsVerificationStatus = null,
            proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
            onMinimiseScreen = { },
            accentId = -1
        )
    }
}
