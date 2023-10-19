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
 */
package com.wire.android.ui.home.conversations.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesButton
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId


@Composable
fun GroupConversationDetailsTopBarCollapsing(
    title: String,
    conversationId: ConversationId,
    totalParticipants: Int,
    isLoading: Boolean,
    onSearchConversationMessagesClick: () -> Unit,
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
                color = colorsScheme().conversationColor(id = conversationId),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing16x,
                            vertical = dimensions().spacing4x
                        )
                ) {
                    Text(
                        text = title.ifBlank {
                            if (isLoading) ""
                            else UIText.StringResource(R.string.group_unavailable_label).asString()
                        },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.wireTypography.body02,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = dimensions().spacing64x)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.conversation_details_participants_count,
                            totalParticipants
                        ),
                        style = MaterialTheme.wireTypography.subline01,
                        color = MaterialTheme.wireColorScheme.secondaryText
                    )
                }
            }
        }

        SearchConversationMessagesButton(
            onSearchConversationMessagesClick = onSearchConversationMessagesClick
        )
    }
}
