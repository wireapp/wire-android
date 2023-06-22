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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
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

    val mentionStyle = SpanStyle(
        color = MaterialTheme.wireColorScheme.onPrimaryVariant,
        background = MaterialTheme.wireColorScheme.primaryVariant
    )

    return remember {
        MessageComposerState(
            context = context,
            isFileSharingEnabled = isFileSharingEnabled,
            interactionAvailability = interactionAvailability,
            securityClassificationType = securityClassificationType,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu,
            selfDeletionTimer = selfDeletionTimer,
            mentionStyle = mentionStyle,
            searchMentions = searchMentions
        )
    }
}

@Suppress("LongParameterList", "TooManyFunctions")
class MessageComposerState(
    context: Context,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO),
    val isFileSharingEnabled: Boolean = true,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    val mentionStyle: SpanStyle,
    onShowEphemeralOptionsMenu: () -> Unit,
    searchMentions: (String) -> Unit
) {
    private val messageCompositionHolder = MessageCompositionHolder(
        context = context,
        mentionStyle = mentionStyle,
        searchMentions = searchMentions
    )

    val messageCompositionInputStateHolder =
        MessageCompositionInputStateHolder(
            selfDeletionTimer = selfDeletionTimer,
            messageCompositionHolder = messageCompositionHolder,
            securityClassificationType = securityClassificationType,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu
        )

    val additionalOptionsStateHolder = AdditionalOptionStateHolder()

    val messageComposition
        get() = messageCompositionHolder.messageComposition.value


    fun toInActive() {
        messageCompositionInputStateHolder.toInActive()
    }

    fun toActive(showAttachmentOption: Boolean) {
        messageCompositionInputStateHolder.toActive(!showAttachmentOption)
        if (showAttachmentOption) {
            additionalOptionsStateHolder.showAdditionalOptionsMenu()
        } else {
            additionalOptionsStateHolder.hideAdditionalOptionsMenu()
        }
    }

    fun toEdit(editMessageText: String, mentions: List<MessageMention>) {
        messageCompositionHolder.setMessageText(TextFieldValue(editMessageText))
        messageCompositionHolder.setMentions(mentions.map { it.toUiMention(editMessageText) })

        messageCompositionInputStateHolder.toEdit()
    }

    fun toSelfDeleting() {
        messageCompositionInputStateHolder.toSelfDeleting()
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)
        messageCompositionInputStateHolder.toComposing()
    }

    fun setMentionSearchResult(mentionSearchResult: List<Contact>) {
        messageCompositionHolder.setMentionsSearchResult(mentionSearchResult)
    }

    fun addMentionToTextMessage(contact: Contact) {
        messageCompositionHolder.addMention(contact)
    }

    fun onMessageTextChanged(messageTextFieldValue: TextFieldValue) {
        messageCompositionHolder.setMessageText(messageTextFieldValue)
    }

    fun MessageMention.toUiMention(originalText: String) = UiMention(
        start = this.start,
        length = this.length,
        userId = this.userId,
        handler = originalText.substring(start, start + length)
    )
}
