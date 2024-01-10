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

package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.AttachmentPickedBundle
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.AudioMessageBundle
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.LocationBundle
import com.wire.android.ui.home.messagecomposer.state.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.ui.home.messagecomposer.state.Ping
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlin.time.Duration

@Composable
fun MessageComposer(
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onSendMessageBundle: (MessageBundle) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
    onTypingEvent: (TypingIndicatorMode) -> Unit
) {
    with(messageComposerStateHolder) {
        when (messageComposerViewState.value.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> {
                DisabledInteractionMessageComposer(
                    conversationId = conversationId,
                    warningText = LocalContext.current.resources.stringWithStyledArgs(
                        R.string.label_system_message_blocked_user,
                        MaterialTheme.wireTypography.body01,
                        MaterialTheme.wireTypography.body02,
                        colorsScheme().secondaryText,
                        colorsScheme().onBackground,
                        stringResource(id = R.string.member_name_you_label_titlecase)
                    ),
                    messageListContent = messageListContent
                )
            }

            InteractionAvailability.DELETED_USER -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = LocalContext.current.resources.stringWithStyledArgs(
                    R.string.label_system_message_user_not_available,
                    MaterialTheme.wireTypography.body01,
                    MaterialTheme.wireTypography.body02,
                    colorsScheme().secondaryText,
                    colorsScheme().onBackground,
                ),
                messageListContent = messageListContent
            )

            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = null,
                messageListContent = messageListContent
            )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    conversationId = conversationId,
                    messageComposerStateHolder = messageComposerStateHolder,
                    messageListContent = messageListContent,
                    onSendButtonClicked = {
                        onSendMessageBundle(messageCompositionHolder.toMessageBundle())
                        onClearMentionSearchResult()
                        clearMessage()
                    },
                    onPingOptionClicked = { onSendMessageBundle(Ping) },
                    onAttachmentPicked = { onSendMessageBundle(AttachmentPickedBundle(it)) },
                    onAudioRecorded = { onSendMessageBundle(AudioMessageBundle(it)) },
                    onLocationPicked = { onSendMessageBundle(LocationBundle(it.getFormattedAddress(), it.location)) },
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                    onClearMentionSearchResult = onClearMentionSearchResult,
                    tempWritableVideoUri = tempWritableVideoUri,
                    tempWritableImageUri = tempWritableImageUri,
                    onTypingEvent = onTypingEvent
                )
            }
        }
    }
}

@Composable
private fun DisabledInteractionMessageComposer(
    conversationId: ConversationId,
    warningText: AnnotatedString?,
    messageListContent: @Composable () -> Unit
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                .fillMaxWidth()
                .weight(1f)

            Box(
                Modifier
                    .background(color = colorsScheme().backgroundVariant)
                    .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
            ) {
                messageListContent()
            }
            if (warningText != null) {
                Divider(color = MaterialTheme.wireColorScheme.outline)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorsScheme().backgroundVariant)
                        .padding(dimensions().spacing16x)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(start = dimensions().spacing8x)
                            .size(dimensions().spacing12x)
                    )
                    Text(
                        text = warningText,
                        style = MaterialTheme.wireTypography.body01,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(start = dimensions().spacing16x)
                    )
                }
            }
            SecurityClassificationBannerForConversation(conversationId = conversationId)
        }
    }
}

@Preview
@Composable
fun MessageComposerPreview() {
    val messageComposerViewState = remember { mutableStateOf(MessageComposerViewState()) }
    val messageComposition = remember { mutableStateOf(MessageComposition.DEFAULT) }
    val selfDeletionTimer = remember { mutableStateOf(SelfDeletionTimer.Enabled(Duration.ZERO)) }

    MessageComposer(
        conversationId = ConversationId("value", "domain"),
        messageComposerStateHolder = MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
                messageComposition = messageComposition,
                selfDeletionTimer = selfDeletionTimer
            ),
            messageCompositionHolder = MessageCompositionHolder(
                context = LocalContext.current
            ),
            additionalOptionStateHolder = AdditionalOptionStateHolder(),
            modalBottomSheetState = WireModalSheetState()
        ),
        messageListContent = { },
        onChangeSelfDeletionClicked = { },
        onSearchMentionQueryChanged = { },
        onClearMentionSearchResult = { },
        onSendMessageBundle = { },
        tempWritableVideoUri = null,
        tempWritableImageUri = null,
        onTypingEvent = { }
    )
}
