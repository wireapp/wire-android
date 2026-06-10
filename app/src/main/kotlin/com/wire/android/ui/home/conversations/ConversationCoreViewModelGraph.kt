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
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModel
import com.wire.android.di.metro.sessionKeyedMetroViewModel
import com.wire.kalium.logic.data.id.ConversationId
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
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface ConversationCoreManualViewModelFactory : ManualViewModelAssistedFactory {
    fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModelImpl
}

@Composable
fun conversationMessagesViewModel(): ConversationMessagesViewModel =
    conversationCoreViewModel()

@Composable
fun messageComposerViewModel(): MessageComposerViewModel =
    conversationCoreViewModel()

@Composable
fun sendMessageViewModel(): SendMessageViewModel =
    conversationCoreViewModel()

@Composable
fun messageDraftViewModel(): MessageDraftViewModel =
    conversationCoreViewModel()

@Composable
fun messageAttachmentsViewModel(): MessageAttachmentsViewModel =
    conversationCoreViewModel()

@Composable
fun conversationMigrationViewModel(): ConversationMigrationViewModel =
    conversationCoreViewModel()

@Composable
fun conversationAssetPathsViewModel(conversationKey: String): ConversationAssetPathsViewModel = when {
    LocalInspectionMode.current -> ConversationAssetPathsViewModelPreview
    else -> conversationCoreViewModel<ConversationAssetPathsViewModelImpl>(key = conversationKey)
}

@Composable
fun mediaGalleryViewModel(): MediaGalleryViewModel =
    conversationCoreViewModel()

@Composable
fun locationPickerViewModel(): LocationPickerViewModel =
    conversationCoreViewModel()

@Composable
fun conversationAssetMessagesViewModel(): ConversationAssetMessagesViewModel =
    conversationCoreViewModel()

@Composable
fun imagesPreviewViewModel(): ImagesPreviewViewModel =
    conversationCoreViewModel()

@Composable
fun messageDetailsViewModel(): MessageDetailsViewModel =
    conversationCoreViewModel()

@Composable
fun quotedMultipartMessageViewModel(conversationKey: String): QuotedMultipartMessageViewModel =
    conversationCoreViewModel(key = conversationKey)

@Composable
fun conversationBannerViewModel(): ConversationBannerViewModel =
    conversationCoreViewModel()

@Composable
fun conversationInfoViewModel(): ConversationInfoViewModel =
    conversationCoreViewModel()

@Composable
fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModel =
    sessionKeyedAssistedMetroViewModel<MultipartAttachmentsViewModelImpl, ConversationCoreManualViewModelFactory>(
        key = conversationId.value,
    ) {
        multipartAttachmentsViewModel(conversationId)
    }

@Composable
private inline fun <reified VM> conversationCoreViewModel(
    key: String? = null,
): VM where VM : ViewModel =
    sessionKeyedMetroViewModel(key)
