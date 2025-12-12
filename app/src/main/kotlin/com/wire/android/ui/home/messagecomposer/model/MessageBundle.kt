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
package com.wire.android.ui.home.messagecomposer.model

import android.location.Location
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.kalium.logic.data.id.ConversationId

sealed class MessageBundle(open val conversationId: ConversationId)

sealed class ComposableMessageBundle(override val conversationId: ConversationId) : MessageBundle(conversationId) {
    data class EditMessageBundle(
        override val conversationId: ConversationId,
        val originalMessageId: String,
        val newContent: String,
        val newMentions: List<UIMention>
    ) : ComposableMessageBundle(conversationId)

    data class EditMultipartMessageBundle(
        override val conversationId: ConversationId,
        val originalMessageId: String,
        val newContent: String,
        val newMentions: List<UIMention>,
    ) : ComposableMessageBundle(conversationId)

    data class SendTextMessageBundle(
        override val conversationId: ConversationId,
        val message: String,
        val mentions: List<UIMention>,
        val quotedMessageId: String? = null
    ) : ComposableMessageBundle(conversationId)

    data class SendMultipartMessageBundle(
        override val conversationId: ConversationId,
        val message: String,
        val mentions: List<UIMention>,
        val quotedMessageId: String? = null
    ) : ComposableMessageBundle(conversationId)

    data class AttachmentPickedBundle(
        override val conversationId: ConversationId,
        val assetBundle: AssetBundle
    ) : ComposableMessageBundle(conversationId)

    data class UriPickedBundle(
        override val conversationId: ConversationId,
        val attachmentUri: UriAsset
    ) : ComposableMessageBundle(conversationId)

    data class AudioMessageBundle(
        override val conversationId: ConversationId,
        val attachmentUri: UriAsset
    ) : ComposableMessageBundle(conversationId)

    data class LocationBundle(
        override val conversationId: ConversationId,
        val locationName: String,
        val location: Location,
        val zoom: Int = 20
    ) : ComposableMessageBundle(conversationId)
}

data class Ping(override val conversationId: ConversationId) : MessageBundle(conversationId)
