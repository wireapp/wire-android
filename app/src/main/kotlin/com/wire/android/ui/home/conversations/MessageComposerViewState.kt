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

import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MessageId

data class MessageComposerViewState(
    val isFileSharingEnabled: Boolean = true,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val mentionSearchResult: List<Contact> = listOf(),
    val mentionSearchQuery: String = String.EMPTY,
    val enterToSend: Boolean = false,
    val isCallOngoing: Boolean = false,
)

sealed class AssetTooLargeDialogState {
    data object Hidden : AssetTooLargeDialogState()
    data class Visible(
        val assetType: AttachmentType,
        val maxLimitInMB: Int,
        val savedToDevice: Boolean,
        val multipleAssets: Boolean = false
    ) : AssetTooLargeDialogState()
}

sealed class VisitLinkDialogState {
    data object Hidden : VisitLinkDialogState()
    data class Visible(val link: String, val openLink: () -> Unit) : VisitLinkDialogState()
}

sealed class InvalidLinkDialogState {
    data object Hidden : InvalidLinkDialogState()
    data object Visible : InvalidLinkDialogState()
}

sealed class SureAboutMessagingDialogState {
    data object Hidden : SureAboutMessagingDialogState()
    sealed class Visible(open val conversationId: ConversationId) : SureAboutMessagingDialogState() {
        data class ConversationVerificationDegraded(
            override val conversationId: ConversationId,
            val messageBundleListToSend: List<MessageBundle>
        ) : Visible(conversationId)

        sealed class ConversationUnderLegalHold(override val conversationId: ConversationId) : Visible(conversationId) {
            data class BeforeSending(
                override val conversationId: ConversationId,
                val messageBundleListToSend: List<MessageBundle>
            ) : ConversationUnderLegalHold(conversationId)

            data class AfterSending(
                override val conversationId: ConversationId,
                val messageIdList: List<MessageId>
            ) : ConversationUnderLegalHold(conversationId)
        }
    }
}

sealed class PermissionPermanentlyDeniedDialogState {
    data object Hidden : PermissionPermanentlyDeniedDialogState()
    data class Visible(val title: Int, val description: Int) : PermissionPermanentlyDeniedDialogState()
}
