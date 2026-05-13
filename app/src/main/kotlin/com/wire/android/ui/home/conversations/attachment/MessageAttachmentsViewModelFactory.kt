/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.attachment

import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class MessageAttachmentsViewModelFactory(
    private val assetImporter: MessageAttachmentAssetImporter,
    private val observeAttachments: ObserveAttachmentDraftsUseCase,
    private val addAttachment: AddAttachmentDraftUseCase,
    private val removeAttachment: RemoveAttachmentDraftUseCase,
    private val retryUpload: RetryAttachmentUploadUseCase,
    private val uploadManager: CellUploadManager,
    private val fileGateway: MessageAttachmentFileGateway,
    private val sharedState: MessageSharedState,
    private val getMediaMetadata: GetMediaMetadataUseCase,
) {
    fun create(args: ConversationNavArgs): MessageAttachmentsViewModel = MessageAttachmentsViewModel(
        conversationNavArgs = args,
        assetImporter = assetImporter,
        observeAttachments = observeAttachments,
        addAttachment = addAttachment,
        removeAttachment = removeAttachment,
        retryUpload = retryUpload,
        uploadManager = uploadManager,
        fileGateway = fileGateway,
        sharedState = sharedState,
        getMediaMetadata = getMediaMetadata,
    )
}
