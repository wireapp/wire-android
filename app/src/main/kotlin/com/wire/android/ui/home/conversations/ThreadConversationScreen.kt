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
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.generated.app.destinations.GroupConversationDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ImagesPreviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.MediaGalleryScreenDestination
import com.ramcosta.composedestinations.generated.sketch.destinations.DrawingCanvasScreenDestination
import com.ramcosta.composedestinations.result.OpenResultRecipient
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs

@WireRootDestination(
    navArgs = ThreadConversationNavArgs::class
)
@Composable
fun ThreadConversationScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient:
    ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    imagePreviewScreenResultRecipient: ResultRecipient<ImagesPreviewScreenDestination, ImagesPreviewNavBackArgs>,
    drawingCanvasScreenResultRecipient: OpenResultRecipient<DrawingCanvasNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    conversationInfoViewModel: ConversationInfoViewModel = hiltViewModel(),
    conversationBannerViewModel: ConversationBannerViewModel = hiltViewModel(),
    conversationCallViewModel: ConversationCallViewModel = hiltViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltViewModel(),
    messageComposerViewModel: MessageComposerViewModel = hiltViewModel(),
    sendMessageViewModel: SendMessageViewModel = hiltViewModel(),
    conversationMigrationViewModel: ConversationMigrationViewModel = hiltViewModel(),
    messageDraftViewModel: MessageDraftViewModel = hiltViewModel(),
    messageAttachmentsViewModel: MessageAttachmentsViewModel = hiltViewModel(),
) {
    ConversationScreenHost(
        screenMode = ConversationScreenMode.Thread,
        navigator = navigator,
        groupDetailsScreenResultRecipient = groupDetailsScreenResultRecipient,
        mediaGalleryScreenResultRecipient = mediaGalleryScreenResultRecipient,
        imagePreviewScreenResultRecipient = imagePreviewScreenResultRecipient,
        drawingCanvasScreenResultRecipient = drawingCanvasScreenResultRecipient,
        resultNavigator = resultNavigator,
        conversationInfoViewModel = conversationInfoViewModel,
        conversationBannerViewModel = conversationBannerViewModel,
        conversationCallViewModel = conversationCallViewModel,
        conversationMessagesViewModel = conversationMessagesViewModel,
        messageComposerViewModel = messageComposerViewModel,
        sendMessageViewModel = sendMessageViewModel,
        conversationMigrationViewModel = conversationMigrationViewModel,
        messageDraftViewModel = messageDraftViewModel,
        messageAttachmentsViewModel = messageAttachmentsViewModel
    )
}
