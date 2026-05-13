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
package com.wire.android.ui.home.gallery

import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import dev.zacsweers.metro.Inject

@Inject
class MediaGalleryViewModelFactory(
    private val getConversationDetails: ObserveConversationDetailsUseCase,
    private val dispatchers: DispatcherProvider,
    private val getImageData: GetMessageAssetUseCase,
    private val fileManager: FileManager,
    private val deleteMessage: DeleteMessageUseCase,
    private val getAttachment: GetMessageAttachmentUseCase,
    private val getCellNode: GetCellFileUseCase,
) {
    fun create(args: MediaGalleryNavArgs): MediaGalleryViewModel = MediaGalleryViewModel(
        mediaGalleryNavArgs = args,
        getConversationDetails = getConversationDetails,
        dispatchers = dispatchers,
        getImageData = getImageData,
        fileManager = fileManager,
        deleteMessage = deleteMessage,
        getAttachment = getAttachment,
        getCellNode = getCellNode,
    )
}
