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

package com.wire.android.ui.home.messagecomposer.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerState(
    isFileSharingEnabled: Boolean = true,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO),
    interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    onShowEphemeralOptionsMenu: () -> Unit,
    searchMentions: (String) -> Unit
): MessageComposerState {
    val context = LocalContext.current

    return remember {
        MessageComposerState(
            context = context,
            isFileSharingEnabled = isFileSharingEnabled,
            interactionAvailability = interactionAvailability,
            securityClassificationType = securityClassificationType,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu,
            selfDeletionTimer = selfDeletionTimer,
            searchMentions = searchMentions
        )
    }
}

@Suppress("LongParameterList", "TooManyFunctions")
class MessageComposerState(
    context: Context,
    val isFileSharingEnabled: Boolean = true,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    onShowEphemeralOptionsMenu: () -> Unit,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO),
    searchMentions: (String) -> Unit
) {
    private val messageCompositionHolder = MessageCompositionHolder(
        context = context,
        requestMentions = searchMentions
    )

    val messageCompositionInputStateHolder =
        MessageCompositionInputStateHolder(
            selfDeletionTimer = selfDeletionTimer,
            messageCompositionHolder = messageCompositionHolder,
            securityClassificationType = securityClassificationType,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu
        )

    val inputState get() = messageCompositionInputStateHolder.inputState

    val inputSize get() = messageCompositionInputStateHolder.inputSize

    val messageComposition
        get() = messageCompositionHolder.messageComposition.value

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        AdditionalOptionSubMenuState.Hidden
    )
        private set

    fun toInActive() {
        messageCompositionInputStateHolder.toInActive()
    }

    fun toActive(showAttachmentOption: Boolean) {
        messageCompositionInputStateHolder.toActive(!showAttachmentOption)
        additionalOptionsSubMenuState = if (showAttachmentOption) {
            AdditionalOptionSubMenuState.AttachFile
        } else {
            AdditionalOptionSubMenuState.Hidden
        }
    }

    fun toEdit(editMessageText: String) {
        messageCompositionHolder.setMessageText(TextFieldValue(editMessageText))
        messageCompositionInputStateHolder.toEdit()

    }

    fun toSelfDeleting() {
        messageCompositionInputStateHolder.toSelfDeleting()
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)
        messageCompositionInputStateHolder.toReply()
    }

    fun setMentionSearchResult(mentionSearchResult: List<Contact>) {
        messageCompositionHolder.setMentionsSearchResult(mentionSearchResult)
    }

    fun onMessageTextChanged(messageTextFieldValue: TextFieldValue) {
        messageCompositionHolder.setMessageText(messageTextFieldValue)
    }

}


fun MessageMention.toUiMention(originalText: String) = UiMention(
    start = this.start,
    length = this.length,
    userId = this.userId,
    handler = originalText.substring(start, start + length)
)
