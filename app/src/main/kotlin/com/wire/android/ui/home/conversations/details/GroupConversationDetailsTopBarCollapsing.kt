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
package com.wire.android.ui.home.conversations.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.info.ConversationAvatar
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.legalhold.banner.LegalHoldSubjectBanner
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun GroupConversationDetailsTopBarCollapsing(
    title: String,
    totalParticipants: Int,
    isUnderLegalHold: Boolean,
    conversationAvatar: ConversationAvatar.Group,
    isWireCellEnabled: Boolean,
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit,
    onLegalHoldLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(contentAlignment = Alignment.Center) {
            GroupConversationAvatar(
                avatarData = conversationAvatar,
                size = dimensions().groupAvatarConversationDetailsTopBarSize,
                cornerRadius = dimensions().groupAvatarConversationDetailsCornerRadius,
                padding = dimensions().avatarConversationTopBarClickablePadding,
            )
        }
        ConstraintLayout(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize()
        ) {
            val (userDescription) = createRefs()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize()
                    .constrainAs(userDescription) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                Text(
                    text = title.ifBlank { UIText.StringResource(R.string.conversation_unavailable_label).asString() },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.wireTypography.body02,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing16x,
                            vertical = dimensions().spacing4x
                        )
                )
                Text(
                    text = stringResource(
                        id = R.string.conversation_details_participants_count,
                        totalParticipants
                    ),
                    style = MaterialTheme.wireTypography.subline01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier
                        .padding(horizontal = dimensions().spacing64x)
                )
                if (isUnderLegalHold) {
                    LegalHoldSubjectBanner(
                        onClick = onLegalHoldLearnMoreClick,
                        modifier = Modifier.padding(vertical = dimensions().spacing8x)
                    )
                }
            }
        }

        VerticalSpace.x24()
        SearchAndMediaRow(
            isWireCellEnabled = isWireCellEnabled,
            onSearchConversationMessagesClick = onSearchConversationMessagesClick,
            onConversationMediaClick = onConversationMediaClick
        )
    }
}

@Composable
fun LoadingGroupConversationDetailsTopBarCollapsing(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(
            modifier = Modifier
                .padding(MaterialTheme.wireDimensions.avatarClickablePadding)
                .size(dimensions().groupAvatarConversationDetailsTopBarSize)
                .padding(dimensions().avatarBorderWidth)
                .background(
                    color = colorsScheme().primaryButtonDisabled,
                    shape = RoundedCornerShape(dimensions().groupAvatarConversationDetailsCornerRadius)
                )
                .shimmerPlaceholder(
                    visible = true,
                    color = colorsScheme().primaryButtonDisabled,
                    shape = RoundedCornerShape(dimensions().groupAvatarConversationDetailsCornerRadius)
                )
        )
        ConstraintLayout(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize()
        ) {
            val (userDescription) = createRefs()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize()
                    .constrainAs(userDescription) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing16x,
                            vertical = dimensions().spacing4x
                        )
                        .height(dimensions().spacing20x)
                        .shimmerPlaceholder(visible = true, color = colorsScheme().primaryButtonDisabled)
                        .fillMaxWidth(0.5f)
                )
                Box(
                    modifier = Modifier
                        .height(dimensions().spacing14x)
                        .padding(horizontal = dimensions().spacing64x)
                        .shimmerPlaceholder(visible = true, color = colorsScheme().primaryButtonDisabled)
                        .fillMaxWidth(0.5f)
                )
            }
        }

        VerticalSpace.x24()
        LoadingSearchAndMediaRow()
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLoadingGroupConversationDetailsTopBarCollapsing() {
    WireTheme {
        LoadingGroupConversationDetailsTopBarCollapsing()
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationDetailsTopBarCollapsing() {
    WireTheme {
        GroupConversationDetailsTopBarCollapsing(
            title = "Conversation Title",
            totalParticipants = 10,
            isUnderLegalHold = true,
            conversationAvatar = ConversationAvatar.Group.Regular(ConversationId("ConversationId", "domain")),
            isWireCellEnabled = false,
            onSearchConversationMessagesClick = {},
            onConversationMediaClick = {},
            onLegalHoldLearnMoreClick = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelConversationDetailsTopBarCollapsing() {
    WireTheme {
        GroupConversationDetailsTopBarCollapsing(
            title = "Conversation Title",
            totalParticipants = 10,
            isUnderLegalHold = true,
            conversationAvatar = ConversationAvatar.Group.Channel(ConversationId("ConversationId", "domain"), false),
            isWireCellEnabled = false,
            onSearchConversationMessagesClick = {},
            onConversationMediaClick = {},
            onLegalHoldLearnMoreClick = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewPrivateChannelConversationDetailsTopBarCollapsing() {
    WireTheme {
        GroupConversationDetailsTopBarCollapsing(
            title = "Conversation Title",
            totalParticipants = 10,
            isUnderLegalHold = true,
            conversationAvatar = ConversationAvatar.Group.Channel(ConversationId("ConversationId", "domain"), true),
            isWireCellEnabled = false,
            onSearchConversationMessagesClick = {},
            onConversationMediaClick = {},
            onLegalHoldLearnMoreClick = {},
        )
    }
}
