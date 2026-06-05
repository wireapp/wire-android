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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
import com.wire.android.ui.home.messagecomposer.AiMessageComposerViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel

interface ConversationCoreViewModelGraph : MetroViewModelGraph {
    val conversationCoreViewModelFactory: ConversationCoreViewModelFactory
}

@Composable
fun conversationMessagesViewModel(): ConversationMessagesViewModel =
    conversationSavedStateViewModel { this.conversationMessagesViewModel(it) }

@Composable
fun messageComposerViewModel(): MessageComposerViewModel =
    conversationSavedStateViewModel { this.messageComposerViewModel(it) }

@Composable
fun sendMessageViewModel(): SendMessageViewModel =
    conversationSavedStateViewModel { this.sendMessageViewModel(it) }

@Composable
fun messageDraftViewModel(): MessageDraftViewModel =
    conversationSavedStateViewModel { this.messageDraftViewModel(it) }

@Composable
fun messageAttachmentsViewModel(): MessageAttachmentsViewModel =
    conversationSavedStateViewModel { this.messageAttachmentsViewModel(it) }

@Composable
fun conversationMigrationViewModel(): ConversationMigrationViewModel =
    conversationSavedStateViewModel { this.conversationMigrationViewModel(it) }

@Composable
fun conversationAssetPathsViewModel(conversationKey: String): ConversationAssetPathsViewModel = when {
    LocalInspectionMode.current -> ConversationAssetPathsViewModelPreview
    else -> conversationViewModel<ConversationAssetPathsViewModelImpl>(key = conversationKey) {
        this.conversationAssetPathsViewModel()
    }
}

@Composable
fun mediaGalleryViewModel(): MediaGalleryViewModel =
    conversationSavedStateViewModel { this.mediaGalleryViewModel(it) }

@Composable
fun locationPickerViewModel(): LocationPickerViewModel =
    conversationViewModel { this.locationPickerViewModel() }

@Composable
fun conversationAssetMessagesViewModel(): ConversationAssetMessagesViewModel =
    conversationSavedStateViewModel { this.conversationAssetMessagesViewModel(it) }

@Composable
fun imagesPreviewViewModel(): ImagesPreviewViewModel =
    conversationSavedStateViewModel { this.imagesPreviewViewModel(it) }

@Composable
fun messageDetailsViewModel(): MessageDetailsViewModel =
    conversationSavedStateViewModel { this.messageDetailsViewModel(it) }

@Composable
fun quotedMultipartMessageViewModel(conversationKey: String): QuotedMultipartMessageViewModel =
    conversationViewModel(key = conversationKey) { this.quotedMultipartMessageViewModel() }

@Composable
fun conversationBannerViewModel(): ConversationBannerViewModel =
    conversationSavedStateViewModel { this.conversationBannerViewModel(it) }

@Composable
fun conversationInfoViewModel(): ConversationInfoViewModel =
    conversationSavedStateViewModel { this.conversationInfoViewModel(it) }

@Composable
fun aiMessageComposerViewModel(): AiMessageComposerViewModel =
    conversationViewModel { this.aiMessageComposerViewModel() }

@Composable
fun multipartAttachmentsViewModel(conversationKey: String): MultipartAttachmentsViewModel =
    conversationViewModel<MultipartAttachmentsViewModelImpl>(key = conversationKey) {
        this.multipartAttachmentsViewModel()
    }

@Composable
private inline fun <reified VM> conversationSavedStateViewModel(
    crossinline create: ConversationCoreViewModelFactory.(SavedStateHandle) -> VM,
): VM where VM : ViewModel =
    metroSavedStateViewModel<ConversationCoreViewModelGraph, VM> {
        conversationCoreViewModelFactory.create(it)
    }

@Composable
private inline fun <reified VM> conversationViewModel(
    key: String? = null,
    crossinline create: ConversationCoreViewModelFactory.() -> VM,
): VM where VM : ViewModel =
    metroViewModel<ConversationCoreViewModelGraph, VM>(key = key) {
        conversationCoreViewModelFactory.create()
    }
