/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.messages

import androidx.lifecycle.ViewModel
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.AttachmentFileType.IMAGE
import com.wire.android.feature.cells.domain.model.AttachmentFileType.VIDEO
import com.wire.android.ui.home.conversations.model.UIMultipartQuotedContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.usecase.ObserveQuoteMessageForConversationUseCase
import com.wire.kalium.logic.data.id.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@HiltViewModel
class QuotedMultipartMessageViewModel @Inject constructor(
    private val observeQuotedMessage: ObserveQuoteMessageForConversationUseCase,
) : ViewModel() {

    suspend fun observeMultipartMessage(conversationId: ConversationId, quotedMessageId: String): Flow<UIQuotedMultipartMessage> =
        observeQuotedMessage(conversationId, quotedMessageId)
            .mapNotNull { result -> (result as? UIQuotedMessage.UIQuotedData)?.quotedContent }
            .filterIsInstance<UIQuotedMessage.UIQuotedData.Multipart>()
            .map { multipart ->
                UIQuotedMultipartMessage(
                    mediaAttachment = if (multipart.attachments.isSingleMediaAttachment()) multipart.attachments.first() else null,
                    fileAttachment = if (multipart.attachments.isSingleFileAttachment()) multipart.attachments.first() else null,
                    attachmentsCount = multipart.attachments.size,
                )
            }
}

data class UIQuotedMultipartMessage(
    val mediaAttachment: UIMultipartQuotedContent? = null,
    val fileAttachment: UIMultipartQuotedContent? = null,
    val attachmentsCount: Int = 0,
)

private fun UIMultipartQuotedContent.isMediaAttachment() =
    when (AttachmentFileType.fromMimeType(mimeType)) {
        IMAGE, VIDEO -> true
        else -> false
    }

private fun List<UIMultipartQuotedContent>.isSingleMediaAttachment() =
    size == 1 && first().isMediaAttachment()

private fun List<UIMultipartQuotedContent>.isSingleFileAttachment() =
    size == 1 && !first().isMediaAttachment()
