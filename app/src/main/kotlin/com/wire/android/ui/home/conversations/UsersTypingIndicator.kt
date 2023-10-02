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
package com.wire.android.ui.home.conversations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID

const val MAX_PREVIEWS_DISPLAY = 3

@Composable
fun UsersTypingIndicatorForConversation(
    conversationId: ConversationId,
    viewModel: TypingIndicatorViewModel = hiltViewModelScoped(conversationId),
) {
    UsersTypingIndicator(usersTyping = viewModel.usersTypingViewState.usersTyping)
}

@Composable
fun UsersTypingIndicator(usersTyping: List<UIParticipant>) {
    if (usersTyping.isNotEmpty()) {
        val rememberTransition =
            rememberInfiniteTransition(label = stringResource(R.string.animation_label_typing_indicator_horizontal_transition))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(dimensions().spacing24x)
                .background(
                    color = colorsScheme().surface,
                    shape = RoundedCornerShape(dimensions().corner14x),
                )
        ) {
            UsersTypingAvatarPreviews(usersTyping)
            Text(
                text = pluralStringResource(
                    R.plurals.typing_indicator_event_message,
                    usersTyping.size,
                    usersTyping.first().name,
                    usersTyping.size - 1
                ),
                style = MaterialTheme.wireTypography.label01.copy(color = colorsScheme().secondaryText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(
                        top = dimensions().spacing4x,
                        bottom = dimensions().spacing4x,
                        end = dimensions().spacing8x,
                    )
            )
            HorizontalBouncingWritingPen(infiniteTransition = rememberTransition)
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun UsersTypingAvatarPreviews(usersTyping: List<UIParticipant>, maxPreviewsDisplay: Int = MAX_PREVIEWS_DISPLAY) {
    usersTyping.take(maxPreviewsDisplay).forEachIndexed { index, user ->
        val isSingleUser = usersTyping.size == 1 || maxPreviewsDisplay == 1
        UserProfileAvatar(
            avatarData = user.avatarData,
            size = dimensions().spacing16x,
            padding = dimensions().spacing2x,
            modifier = if (isSingleUser) Modifier
            else {
                Modifier.offset(
                    x = if (index == 0) dimensions().spacing8x else -(dimensions().spacing6x)
                )
            }
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun HorizontalBouncingWritingPen(
    infiniteTransition: InfiniteTransition,
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        val position by infiniteTransition.animateFloat(
            initialValue = -5f, targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = infiniteTransition.label
        )

        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = colorsScheme().secondaryText,
            modifier = Modifier
                .size(dimensions().spacing12x)
                .offset(y = -dimensions().spacing2x)
                .align(Alignment.Bottom)
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = colorsScheme().secondaryText,
            modifier = Modifier
                .size(dimensions().spacing12x)
                .offset(x = position.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUsersTypingOne() {
    Column(
        modifier = Modifier
            .background(color = colorsScheme().background)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UsersTypingIndicator(
            listOf(
                UIParticipant(
                    id = QualifiedID("Alice", "wire.com"),
                    name = "Alice",
                    handle = "alice",
                    isSelf = false,
                    isService = false,
                    avatarData = UserAvatarData(),
                    membership = Membership.None,
                    connectionState = null,
                    unavailable = false,
                    isDeleted = false,
                    readReceiptDate = null,
                    botService = null,
                    isDefederated = false
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUsersTypingMoreThanOne() {
    Column(
        modifier = Modifier
            .background(color = colorsScheme().background)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UsersTypingIndicator(
            listOf(
                UIParticipant(
                    id = QualifiedID("Bob", "wire.com"),
                    name = "Bob",
                    handle = "bob",
                    isSelf = false,
                    isService = false,
                    avatarData = UserAvatarData(),
                    membership = Membership.None,
                    connectionState = null,
                    unavailable = false,
                    isDeleted = false,
                    readReceiptDate = null,
                    botService = null,
                    isDefederated = false
                ),
                UIParticipant(
                    id = QualifiedID("alice", "wire.com"),
                    name = "Alice Smith",
                    handle = "alice",
                    isSelf = false,
                    isService = false,
                    avatarData = UserAvatarData(),
                    membership = Membership.None,
                    connectionState = null,
                    unavailable = false,
                    isDeleted = false,
                    readReceiptDate = null,
                    botService = null,
                    isDefederated = false
                )
            )
        )
    }
}
