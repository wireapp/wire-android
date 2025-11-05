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

package com.wire.android.ui.home.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import com.wire.android.R
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.controlbuttons.JoinButton
import com.wire.android.ui.calling.controlbuttons.StartCallButton
import com.wire.android.ui.common.ConversationVerificationIcons
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.topappbar.NavigationIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.home.conversations.info.ConversationAvatar
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversationslist.common.ChannelConversationAvatar
import com.wire.android.ui.home.conversationslist.common.RegularGroupConversationAvatar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun ConversationScreenTopAppBar(
    conversationInfoViewState: ConversationInfoViewState,
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit,
    hasOngoingCall: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit,
    isInteractionEnabled: Boolean,
    isDropDownEnabled: Boolean = false,
    containerColor: Color? = null
) {
    val featureVisibilityFlags = LocalFeatureVisibilityFlags.current
    ConversationScreenTopAppBarContent(
        conversationInfoViewState = conversationInfoViewState,
        onBackButtonClick = onBackButtonClick,
        onDropDownClick = onDropDownClick,
        isDropDownEnabled = isDropDownEnabled,
        onSearchButtonClick = onSearchButtonClick,
        onPhoneButtonClick = onPhoneButtonClick,
        hasOngoingCall = hasOngoingCall,
        onJoinCallButtonClick = onJoinCallButtonClick,
        onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
        isInteractionEnabled = isInteractionEnabled,
        isSearchEnabled = featureVisibilityFlags.ConversationSearchIcon,
        containerColor = containerColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreenTopAppBarContent(
    conversationInfoViewState: ConversationInfoViewState,
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit,
    hasOngoingCall: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit,
    isInteractionEnabled: Boolean,
    isSearchEnabled: Boolean,
    isDropDownEnabled: Boolean = false,
    containerColor: Color? = null
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // TopAppBar adds TopAppBarHorizontalPadding = 4.dp to each element, so we need to offset it to retain the desired
                    // spacing between navigation icon button and avatar according to the designs
                    .offset(x = -dimensions().spacing4x)
                    .clip(RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize))
                    .clickable(
                        onClick = onDropDownClick,
                        enabled = isDropDownEnabled && isInteractionEnabled,
                        onClickLabel = stringResource(R.string.content_description_conversation_open_details_label)
                    )
            ) {
                val conversationAvatar: ConversationAvatar = conversationInfoViewState.conversationAvatar
                Avatar(conversationAvatar, conversationInfoViewState)
                Text(
                    text = conversationInfoViewState.conversationName.asString(),
                    style = MaterialTheme.wireTypography.title02,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                ConversationVerificationIcons(
                    conversationInfoViewState.protocolInfo,
                    conversationInfoViewState.mlsVerificationStatus,
                    conversationInfoViewState.proteusVerificationStatus
                )
                if (conversationInfoViewState.legalHoldStatus == Conversation.LegalHoldStatus.ENABLED) {
                    HorizontalSpace.x4()
                    LegalHoldIndicator()
                }
                if (isDropDownEnabled && isInteractionEnabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dropdown_icon),
                        contentDescription = null
                    )
                }
            }
        },
        navigationIcon = {
            NavigationIconButton(
                NavigationIconType.Back(R.string.content_description_conversation_back_btn),
                onBackButtonClick
            )
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
                modifier = Modifier.padding(end = dimensions().spacing4x)
            ) {
                if (isSearchEnabled) {
                    WireSecondaryIconButton(
                        onButtonClicked = onSearchButtonClick,
                        iconResource = R.drawable.ic_search,
                        contentDescription = R.string.content_description_conversation_search_icon,
                        minSize = dimensions().buttonSmallMinSize,
                        minClickableSize = DpSize(
                            dimensions().buttonSmallMinSize.width,
                            dimensions().buttonMinClickableSize.height
                        ),
                    )
                }

                CallControlButton(
                    hasOngoingCall = hasOngoingCall,
                    onJoinCallButtonClick = onJoinCallButtonClick,
                    onPhoneButtonClick = onPhoneButtonClick,
                    isCallingEnabled = isInteractionEnabled,
                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.background,
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
        is ConversationAvatar.Group.Regular ->
            RegularGroupConversationAvatar(
                conversationId = conversationAvatar.conversationId,
                size = dimensions().avatarConversationTopBarSize,
                cornerRadius = dimensions().groupAvatarConversationTopBarCornerRadius,
                padding = dimensions().avatarConversationTopBarClickablePadding,
            )

        is ConversationAvatar.Group.Channel ->
            ChannelConversationAvatar(
                conversationId = conversationAvatar.conversationId,
                size = dimensions().avatarConversationTopBarSize,
                cornerRadius = dimensions().groupAvatarConversationTopBarCornerRadius,
                padding = dimensions().avatarConversationTopBarClickablePadding,
                isPrivateChannel = conversationAvatar.isPrivate,
            )

        is ConversationAvatar.OneOne -> UserProfileAvatar(
            avatarData = UserAvatarData(
                asset = conversationAvatar.avatarAsset,
                availabilityStatus = conversationAvatar.status,
                connectionState = (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.OneOne)?.connectionState,
                nameBasedAvatar = NameBasedAvatar(
                    fullName = conversationInfoViewState.conversationName.asString(),
                    accentColor = conversationInfoViewState.accentId
                )
            ),
            size = dimensions().avatarConversationTopBarSize,
            padding = dimensions().avatarConversationTopBarClickablePadding,
        )

        ConversationAvatar.None -> Box(modifier = Modifier.size(dimensions().avatarConversationTopBarSize))
    }
}

@Composable
private fun CallControlButton(
    hasOngoingCall: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit,
    onPhoneButtonClick: () -> Unit,
    isCallingEnabled: Boolean
) {
    if (hasOngoingCall) {
        JoinButton(
            buttonClick = onJoinCallButtonClick,
            onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
        )
    } else {
        StartCallButton(
            onPhoneButtonClick = onPhoneButtonClick,
            onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
            isCallingEnabled = isCallingEnabled
        )
    }
}

@Preview("Topbar with a very long conversation title")
@Composable
fun PreviewConversationScreenTopAppBarLongTitle() {
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString(
                "This is some very very very very very very very very very very long conversation title"
            ),
            conversationDetailsData = ConversationDetailsData.Group(
                conversationProtocol = null,
                conversationId = QualifiedID("", "")
            ),
            conversationAvatar = ConversationAvatar.OneOne(null, UserAvailabilityStatus.NONE),
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}

@Preview("Topbar with a very long conversation title and search button")
@Composable
fun PreviewConversationScreenTopAppBarLongTitleWithSearch() {
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString(
                "This is some very very very very very very very very very very long conversation title"
            ),
            conversationDetailsData = ConversationDetailsData.Group(
                conversationProtocol = null,
                conversationId = QualifiedID("", "")
            ),
            conversationAvatar = ConversationAvatar.OneOne(null, UserAvailabilityStatus.NONE),
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = true
    )
}

@Preview("Topbar with a very long conversation title and search button and join group call")
@Composable
fun PreviewConversationScreenTopAppBarLongTitleWithSearchAndOngoingCall() {
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString(
                "This is some very very very very very very very very very very long conversation title"
            ),
            conversationDetailsData = ConversationDetailsData.Group(
                conversationProtocol = null,
                conversationId = QualifiedID("", "")
            ),
            conversationAvatar = ConversationAvatar.OneOne(null, UserAvailabilityStatus.NONE),
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = true,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = true
    )
}

@Preview("Topbar with a short conversation title")
@Composable
fun PreviewConversationScreenTopAppBarShortTitle() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(null, conversationId),
            conversationAvatar = ConversationAvatar.Group.Regular(conversationId)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}

@MultipleThemePreviews
@Preview("Topbar with a short conversation title for a channel")
@Composable
fun PreviewConversationScreenTopAppBarShortTitleChannel() = WireTheme {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(null, conversationId),
            conversationAvatar = ConversationAvatar.Group.Channel(conversationId, true)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}

@Preview("Topbar with a short conversation title and join group call")
@Composable
fun PreviewConversationScreenTopAppBarShortTitleWithOngoingCall() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(null, conversationId),
            conversationAvatar = ConversationAvatar.Group.Regular(conversationId)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = true,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}

@Preview("Topbar with a short conversation title and verified")
@Composable
fun PreviewConversationScreenTopAppBarShortTitleWithVerified() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(null, conversationId),
            conversationAvatar = ConversationAvatar.Group.Regular(conversationId),
            protocolInfo = Conversation.ProtocolInfo.Proteus,
            proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
            mlsVerificationStatus = Conversation.VerificationStatus.VERIFIED
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}

@Preview("Topbar with a short conversation title and verified")
@Composable
fun PreviewConversationScreenTopAppBarShortTitleWithLegalHold() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBarContent(
        ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Short title"),
            conversationDetailsData = ConversationDetailsData.Group(null, conversationId),
            conversationAvatar = ConversationAvatar.Group.Regular(conversationId),
            protocolInfo = Conversation.ProtocolInfo.Proteus,
            legalHoldStatus = Conversation.LegalHoldStatus.ENABLED,
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        onJoinCallButtonClick = {},
        onAudioPermissionPermanentlyDenied = {},
        isInteractionEnabled = true,
        isSearchEnabled = false
    )
}
