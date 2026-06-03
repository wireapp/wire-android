@file:Suppress("TooManyFunctions")

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
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModel
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelPreview
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelImpl
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel

interface ConversationCoreViewModelGraph : MetroViewModelGraph {
    val conversationCoreViewModelFactory: ConversationCoreViewModelFactory
}

@Composable
fun conversationMessagesViewModel(): ConversationMessagesViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ConversationMessagesViewModel> {
        conversationCoreViewModelFactory.conversationMessagesViewModel(it)
    }

@Composable
fun messageComposerViewModel(): MessageComposerViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, MessageComposerViewModel> {
        conversationCoreViewModelFactory.messageComposerViewModel(it)
    }

@Composable
fun sendMessageViewModel(): SendMessageViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, SendMessageViewModel> {
        conversationCoreViewModelFactory.sendMessageViewModel(it)
    }

@Composable
fun messageDraftViewModel(): MessageDraftViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, MessageDraftViewModel> {
        conversationCoreViewModelFactory.messageDraftViewModel(it)
    }

@Composable
fun messageAttachmentsViewModel(): MessageAttachmentsViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, MessageAttachmentsViewModel> {
        conversationCoreViewModelFactory.messageAttachmentsViewModel(it)
    }

@Composable
fun conversationMigrationViewModel(): ConversationMigrationViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ConversationMigrationViewModel> {
        conversationCoreViewModelFactory.conversationMigrationViewModel(it)
    }

@Composable
fun conversationAssetPathsViewModel(conversationKey: String): ConversationAssetPathsViewModel = when {
    LocalInspectionMode.current -> ConversationAssetPathsViewModelPreview
    else -> metroViewModel<ConversationCoreViewModelGraph, ConversationAssetPathsViewModelImpl>(key = conversationKey) {
        conversationCoreViewModelFactory.conversationAssetPathsViewModel()
    }
}

@Composable
fun mediaGalleryViewModel(): MediaGalleryViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, MediaGalleryViewModel> {
        conversationCoreViewModelFactory.mediaGalleryViewModel(it)
    }

@Composable
fun locationPickerViewModel(): LocationPickerViewModel =
    metroViewModel<ConversationCoreViewModelGraph, LocationPickerViewModel> {
        conversationCoreViewModelFactory.locationPickerViewModel()
    }

@Composable
fun conversationAssetMessagesViewModel(): ConversationAssetMessagesViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ConversationAssetMessagesViewModel> {
        conversationCoreViewModelFactory.conversationAssetMessagesViewModel(it)
    }

@Composable
fun imagesPreviewViewModel(): ImagesPreviewViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ImagesPreviewViewModel> {
        conversationCoreViewModelFactory.imagesPreviewViewModel(it)
    }

@Composable
fun messageDetailsViewModel(): MessageDetailsViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, MessageDetailsViewModel> {
        conversationCoreViewModelFactory.messageDetailsViewModel(it)
    }

@Composable
fun quotedMultipartMessageViewModel(conversationKey: String): QuotedMultipartMessageViewModel =
    metroViewModel<ConversationCoreViewModelGraph, QuotedMultipartMessageViewModel>(key = conversationKey) {
        conversationCoreViewModelFactory.quotedMultipartMessageViewModel()
    }

@Composable
fun conversationBannerViewModel(): ConversationBannerViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ConversationBannerViewModel> {
        conversationCoreViewModelFactory.conversationBannerViewModel(it)
    }

@Composable
fun conversationInfoViewModel(): ConversationInfoViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, ConversationInfoViewModel> {
        conversationCoreViewModelFactory.conversationInfoViewModel(it)
    }

@Composable
fun multipartAttachmentsViewModel(conversationKey: String): MultipartAttachmentsViewModel =
    metroViewModel<ConversationCoreViewModelGraph, MultipartAttachmentsViewModelImpl>(key = conversationKey) {
        conversationCoreViewModelFactory.multipartAttachmentsViewModel()
    }
