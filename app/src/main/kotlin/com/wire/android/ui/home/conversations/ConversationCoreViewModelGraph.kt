@file:Suppress("MatchingDeclarationName", "TooManyFunctions")

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
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModel
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.media.ConversationMediaNavArgs
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModel
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavArgs
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelPreview
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelImpl
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.gallery.MediaGalleryNavArgs
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup

@WireAssistedViewModelFactoryGroup
object ConversationCoreManualViewModelFactoryGroup

@Composable
fun conversationMessagesViewModel(): ConversationMessagesViewModel =
    conversationCoreViewModel()

@Composable
fun conversationMessagesViewModel(args: ConversationNavArgs): ConversationMessagesViewModel =
    wireAssistedMetroViewModel<ConversationMessagesViewModel, ConversationCoreManualViewModelFactory> { _ ->
        conversationMessagesViewModel(args)
    }

@Composable
fun messageComposerViewModel(): MessageComposerViewModel =
    conversationCoreViewModel()

@Composable
fun messageComposerViewModel(args: ConversationNavArgs): MessageComposerViewModel =
    wireAssistedMetroViewModel<MessageComposerViewModel, ConversationCoreManualViewModelFactory> { _ ->
        messageComposerViewModel(args)
    }

@Composable
fun sendMessageViewModel(): SendMessageViewModel =
    conversationCoreViewModel()

@Composable
fun sendMessageViewModel(args: ConversationNavArgs): SendMessageViewModel =
    wireAssistedMetroViewModel<SendMessageViewModel, ConversationCoreManualViewModelFactory> { _ ->
        sendMessageViewModel(args)
    }

@Composable
fun messageDraftViewModel(): MessageDraftViewModel =
    conversationCoreViewModel()

@Composable
fun messageDraftViewModel(args: ConversationNavArgs): MessageDraftViewModel =
    wireAssistedMetroViewModel<MessageDraftViewModel, ConversationCoreManualViewModelFactory> { _ ->
        messageDraftViewModel(args)
    }

@Composable
fun messageAttachmentsViewModel(): MessageAttachmentsViewModel =
    conversationCoreViewModel()

@Composable
fun messageAttachmentsViewModel(args: ConversationNavArgs): MessageAttachmentsViewModel =
    wireAssistedMetroViewModel<MessageAttachmentsViewModel, ConversationCoreManualViewModelFactory> { _ ->
        messageAttachmentsViewModel(args)
    }

@Composable
fun conversationAssetPathsViewModel(conversationKey: String): ConversationAssetPathsViewModel = when {
    LocalInspectionMode.current -> ConversationAssetPathsViewModelPreview
    else -> conversationCoreViewModel<ConversationAssetPathsViewModelImpl>(key = conversationKey)
}

@Composable
fun mediaGalleryViewModel(): MediaGalleryViewModel =
    conversationCoreViewModel()

@Composable
fun mediaGalleryViewModel(args: MediaGalleryNavArgs): MediaGalleryViewModel =
    wireAssistedMetroViewModel<MediaGalleryViewModel, ConversationCoreManualViewModelFactory> { _ ->
        mediaGalleryViewModel(args)
    }

@Composable
fun locationPickerViewModel(): LocationPickerViewModel =
    conversationCoreViewModel()

@Composable
fun conversationAssetMessagesViewModel(): ConversationAssetMessagesViewModel =
    conversationCoreViewModel()

@Composable
fun conversationAssetMessagesViewModel(args: ConversationMediaNavArgs): ConversationAssetMessagesViewModel =
    wireAssistedMetroViewModel<ConversationAssetMessagesViewModel, ConversationCoreManualViewModelFactory> { _ ->
        conversationAssetMessagesViewModel(args)
    }

@Composable
fun imagesPreviewViewModel(): ImagesPreviewViewModel =
    conversationCoreViewModel()

@Composable
fun imagesPreviewViewModel(args: ImagesPreviewNavArgs): ImagesPreviewViewModel =
    wireAssistedMetroViewModel<ImagesPreviewViewModel, ConversationCoreManualViewModelFactory> { _ ->
        imagesPreviewViewModel(args)
    }

@Composable
fun quotedMultipartMessageViewModel(conversationKey: String): QuotedMultipartMessageViewModel =
    conversationCoreViewModel(key = conversationKey)

@Composable
fun conversationBannerViewModel(): ConversationBannerViewModel =
    conversationCoreViewModel()

@Composable
fun conversationBannerViewModel(args: ConversationNavArgs): ConversationBannerViewModel =
    wireAssistedMetroViewModel<ConversationBannerViewModel, ConversationCoreManualViewModelFactory> { _ ->
        conversationBannerViewModel(args)
    }

@Composable
fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModel =
    wireAssistedMetroViewModel<MultipartAttachmentsViewModelImpl, ConversationCoreManualViewModelFactory>(
        instanceKey = conversationId.value,
    ) { _ ->
        multipartAttachmentsViewModel(conversationId)
    }

@Composable
private inline fun <reified VM> conversationCoreViewModel(
    key: String? = null,
): VM where VM : ViewModel =
    wireMetroViewModel(instanceKey = key)
