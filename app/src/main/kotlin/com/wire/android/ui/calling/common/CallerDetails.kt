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

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.common.ConversationVerificationIcons
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import java.util.Locale

@Composable
fun CallerDetails(
    conversationId: ConversationId,
    conversationName: ConversationName?,
    isCameraOn: Boolean,
    isCbrEnabled: Boolean,
    avatarAssetId: ImageAsset.UserAvatarAsset?,
    conversationType: ConversationType,
    membership: Membership,
    callingLabel: String,
    protocolInfo: Conversation.ProtocolInfo?,
    mlsVerificationStatus: Conversation.VerificationStatus?,
    proteusVerificationStatus: Conversation.VerificationStatus?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = dimensions().spacing32x),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        IconButton(
            modifier = Modifier
                .padding(top = dimensions().spacing16x, start = dimensions().spacing6x)
                .align(Alignment.Start)
                .rotate(180f),
            onClick = {
                Toast.makeText(context, "Not implemented yet =)", Toast.LENGTH_SHORT).show()
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(id = R.string.calling_minimize_view),
            )
        }
        if (isCbrEnabled) {
            Text(
                text = stringResource(id = R.string.calling_constant_bit_rate_indication).uppercase(Locale.getDefault()),
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
        Text(
            text = callingLabel,
            color = colorsScheme().onBackground,
            style = MaterialTheme.wireTypography.body01,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )
        if (membership.hasLabel()) {
            VerticalSpace.x16()
            MembershipQualifierLabel(membership)
        }

        SecurityClassificationBannerForConversation(
            conversationId = conversationId,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )

        if (!isCameraOn && conversationType == ConversationType.OneOnOne) {
            UserProfileAvatar(
                avatarData = UserAvatarData(avatarAssetId),
                size = dimensions().initiatingCallUserAvatarSize,
                modifier = Modifier.padding(top = dimensions().spacing16x)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCallerDetails() {
    CallerDetails(
        conversationId = ConversationId("value", "domain"),
        conversationName = ConversationName.Known("User"),
        isCameraOn = false,
        isCbrEnabled = false,
        avatarAssetId = null,
        conversationType = ConversationType.OneOnOne,
        membership = Membership.Guest,
        callingLabel = String.EMPTY,
        protocolInfo = null,
        mlsVerificationStatus = null,
        proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED
    )
}
