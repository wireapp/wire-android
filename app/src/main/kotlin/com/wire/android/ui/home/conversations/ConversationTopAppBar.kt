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

package com.wire.android.ui.home.conversations

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.controlbuttons.JoinButton
import com.wire.android.ui.calling.controlbuttons.StartCallButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.BackNavigationIconButton
import com.wire.android.ui.home.conversations.info.ConversationAvatar
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationVerificationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.conversation.ConversationProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreenTopAppBar(
    conversationInfoViewState: ConversationInfoViewState,
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    isDropDownEnabled: Boolean = false,
    onSearchButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit,
    hasOngoingCall: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onPermanentPermissionDecline: () -> Unit,
    isInteractionEnabled: Boolean,
) {
    SmallTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize))
                    .clickable(onClick = onDropDownClick, enabled = isDropDownEnabled && isInteractionEnabled)

            ) {
                val conversationAvatar: ConversationAvatar = conversationInfoViewState.conversationAvatar
                Avatar(conversationAvatar, conversationInfoViewState)
                Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
                Text(
                    text = conversationInfoViewState.conversationName.asString(),
                    style = MaterialTheme.wireTypography.title01,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                if (conversationInfoViewState.verificationStatus?.status == ConversationVerificationStatus.VERIFIED) {
                    val (iconId, contentDescriptionId) = when (conversationInfoViewState.verificationStatus.protocol) {
                        ConversationProtocol.MLS ->
                            R.drawable.ic_certificate_valid_mls to R.string.content_description_mls_certificate_valid

                        ConversationProtocol.PROTEUS ->
                            R.drawable.ic_certificate_valid_proteus to R.string.content_description_proteus_certificate_valid
                    }
                    Image(
                        modifier = Modifier.padding(start = dimensions().spacing4x),
                        painter = painterResource(id = iconId),
                        contentDescription = stringResource(contentDescriptionId)
                    )
                }
                if (isDropDownEnabled && isInteractionEnabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dropdown_icon),
                        contentDescription = stringResource(R.string.content_description_drop_down_icon)
                    )
                }
            }
        },
        navigationIcon = { BackNavigationIconButton(onBackButtonClick = onBackButtonClick) },
        actions = {
            val featureVisibilityFlags = LocalFeatureVisibilityFlags.current

            if (featureVisibilityFlags.ConversationSearchIcon) {
                WireSecondaryButton(
                    onClick = onSearchButtonClick,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    fillMaxWidth = false,
                    minHeight = MaterialTheme.wireDimensions.spacing32x,
                    minWidth = MaterialTheme.wireDimensions.spacing40x,
                    shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
                    contentPadding = PaddingValues(0.dp)
                )
            }

            CallControlButton(
                hasOngoingCall = hasOngoingCall,
                onJoinCallButtonClick = onJoinCallButtonClick,
                onPermanentPermissionDecline = onPermanentPermissionDecline,
                onPhoneButtonClick = onPhoneButtonClick,
                isCallingEnabled = isInteractionEnabled
            )
            Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun Avatar(
    conversationAvatar: ConversationAvatar,
    conversationInfoViewState: ConversationInfoViewState
) {
    when (conversationAvatar) {
        is ConversationAvatar.Group ->
            GroupConversationAvatar(
                color = colorsScheme().conversationColor(id = conversationAvatar.conversationId)
            )

        is ConversationAvatar.OneOne -> UserProfileAvatar(
            UserAvatarData(
                asset = conversationAvatar.avatarAsset,
                availabilityStatus = conversationAvatar.status,
                connectionState = (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.OneOne)?.connectionState
            )
        )

        ConversationAvatar.None -> Box(modifier = Modifier.size(dimensions().userAvatarDefaultSize))
    }
}

@Composable
private fun CallControlButton(
    hasOngoingCall: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onPermanentPermissionDecline: () -> Unit,
    onPhoneButtonClick: () -> Unit,
    isCallingEnabled: Boolean
) {
    if (hasOngoingCall) {
        JoinButton(
            buttonClick = onJoinCallButtonClick,
            onPermanentPermissionDecline = onPermanentPermissionDecline,
            minHeight = MaterialTheme.wireDimensions.spacing28x
        )
    } else {
        StartCallButton(
            onPhoneButtonClick = onPhoneButtonClick,
            onPermanentPermissionDecline = onPermanentPermissionDecline,
            isCallingEnabled = isCallingEnabled
        )
    }
}

@Preview("Topbar with a very long conversation title")
@Composable
fun PreviewConversationScreenTopAppBarLongTitle() {
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString(
                "This is some very very very very very very very very very very long conversation title"
            ),
            conversationDetailsData = ConversationDetailsData.Group(QualifiedID("", "")),
            conversationAvatar = ConversationAvatar.OneOne(null, UserAvailabilityStatus.NONE),
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onPermanentPermissionDecline = {},
        isInteractionEnabled = true
    )
}

@Preview("Topbar with a short  conversation title")
@Composable
fun PreviewConversationScreenTopAppBarShortTitle() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(conversationId),
            conversationAvatar = ConversationAvatar.Group(conversationId)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onPermanentPermissionDecline = {},
        isInteractionEnabled = true
    )
}

@Preview("Topbar with a short  conversation title and join group call")
@Composable
fun PreviewConversationScreenTopAppBarShortTitleWithOngoingCall() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(conversationId),
            conversationAvatar = ConversationAvatar.Group(conversationId)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = true,
        onJoinCallButtonClick = {},
        onPermanentPermissionDecline = {},
        isInteractionEnabled = true
    )
}
