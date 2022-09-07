package com.wire.android.ui.home.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wire.android.ui.calling.controlButtons.JoinButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.BackNavigationIconButton
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun ConversationScreenTopAppBar(
    conversationInfoViewState: ConversationInfoViewState,
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    isDropDownEnabled: Boolean = false,
    onSearchButtonClick: () -> Unit = {},
    onPhoneButtonClick: () -> Unit = {},
    hasOngoingCall: Boolean,
    isUserBlocked: Boolean,
    onJoinCallButtonClick: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize))
                    .clickable(onClick = onDropDownClick, enabled = isDropDownEnabled)

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
                if (isDropDownEnabled)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dropdown_icon),
                        contentDescription = stringResource(R.string.content_description_drop_down_icon)
                    )
            }
        },
        navigationIcon = { BackNavigationIconButton(onBackButtonClick = onBackButtonClick) },
        actions = {
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
            Spacer(Modifier.width(MaterialTheme.wireDimensions.spacing6x))
            callControlButton(
                hasOngoingCall = hasOngoingCall,
                onJoinCallButtonClick = onJoinCallButtonClick,
                onPhoneButtonClick = onPhoneButtonClick,
                isUserBlocked = isUserBlocked
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
private fun callControlButton(
    hasOngoingCall: Boolean,
    isUserBlocked: Boolean,
    onJoinCallButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit
) {
    if (hasOngoingCall) {
        JoinButton(
            buttonClick = onJoinCallButtonClick,
            minHeight = MaterialTheme.wireDimensions.spacing28x
        )
    } else {
        WireSecondaryButton(
            onClick = onPhoneButtonClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(R.string.content_description_conversation_phone_icon),
                )
            },
            state = if (isUserBlocked) WireButtonState.Disabled else WireButtonState.Default,
            fillMaxWidth = false,
            minHeight = MaterialTheme.wireDimensions.spacing32x,
            minWidth = MaterialTheme.wireDimensions.spacing40x,
            shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview("Topbar with a very long conversation title")
@Composable
fun ConversationScreenTopAppBarLongTitlePreview() {
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
            conversationName = UIText.DynamicString(
                "This is some very very very very very very very very very very long conversation title"
            ),
            conversationDetailsData = ConversationDetailsData.Group(QualifiedID("", "")),
            conversationAvatar = ConversationAvatar.OneOne(null, UserAvailabilityStatus.NONE)
        ),
        onBackButtonClick = {},
        onDropDownClick = {},
        isDropDownEnabled = true,
        onSearchButtonClick = {},
        onPhoneButtonClick = {},
        hasOngoingCall = false,
        isUserBlocked = false,
        onJoinCallButtonClick = {}
    )
}

@Preview("Topbar with a short  conversation title")
@Composable
fun ConversationScreenTopAppBarShortTitlePreview() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
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
        isUserBlocked = false,
        onJoinCallButtonClick = {}
    )
}

@Preview("Topbar with a short  conversation title and join group call")
@Composable
fun ConversationScreenTopAppBarShortTitleWithOngoingCallPreview() {
    val conversationId = QualifiedID("", "")
    ConversationScreenTopAppBar(
        ConversationInfoViewState(
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
        isUserBlocked = false,
        onJoinCallButtonClick = {}
    )
}
