/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui.home.conversations

import android.net.Uri
import androidx.compose.runtime.Composable
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
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.userprofile.service.ServiceDetailsNavArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Suppress("TooManyFunctions")
internal interface ConversationRouteScreenNavigation {
    fun goBack()
    fun replaceConversation(conversationId: ConversationId)
    fun openSelfUserProfile()
    fun openOtherUserProfile(userId: UserId, conversationId: ConversationId? = null)
    fun openServiceDetails(args: ServiceDetailsNavArgs)
    fun openMessageDetails(conversationId: ConversationId, messageId: String, isSelfMessage: Boolean)
    fun openGroupDetails(
        conversationId: ConversationId,
    )
    fun openImagesPreview(
        conversationId: ConversationId,
        conversationName: String,
        assetUris: List<Uri>,
    )

    @Suppress("LongParameterList")
    fun openMediaGallery(
        conversationId: ConversationId,
        messageId: String,
        isSelfAsset: Boolean,
        isEphemeral: Boolean,
        messageOptionsEnabled: Boolean,
        cellAssetId: String?,
    )

    fun openVideoPlayer(localPath: String?, contentUrl: String?, fileName: String?)
    fun openAudioPlayer(localPath: String?, contentUrl: String?, fileName: String?)

    fun openPdfViewer(localPath: String?, assetId: String?, remotePath: String?, assetSize: Long, fileName: String?)

    fun openDrawingCanvas(
        conversationName: String,
        tempWritableUri: Uri?,
    )
    fun onGroupDetailsResult(handler: (GroupConversationDetailsNavBackArgs?) -> Unit)
    fun onImagesPreviewResult(handler: (ImagesPreviewNavBackArgs?) -> Unit)
    fun onMediaGalleryResult(handler: (MediaGalleryNavBackArgs?) -> Unit)
    fun onDrawingCanvasResult(handler: (Uri?) -> Unit)
    fun completeConversation(result: GroupConversationDetailsNavBackArgs)
}

internal class ConversationRouteResultHandlers {
    var groupDetails: (GroupConversationDetailsNavBackArgs?) -> Unit = {}
    var imagesPreview: (ImagesPreviewNavBackArgs?) -> Unit = {}
    var mediaGallery: (MediaGalleryNavBackArgs?) -> Unit = {}
    var drawingCanvas: (Uri?) -> Unit = {}
}

@Suppress("ComposeViewModelForwarding")
@Composable
internal fun ConversationRouteScreen(
    navigation: ConversationRouteScreenNavigation,
    onShareAssetViaWire: (Uri) -> Unit,
    conversationInfoViewModel: ConversationInfoViewModel,
    conversationBannerViewModel: ConversationBannerViewModel,
    conversationCallViewModel: ConversationCallViewModel,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    messageComposerViewModel: MessageComposerViewModel,
    sendMessageViewModel: SendMessageViewModel,
    conversationMigrationViewModel: ConversationMigrationViewModel,
    messageDraftViewModel: MessageDraftViewModel,
    messageAttachmentsViewModel: MessageAttachmentsViewModel,
) {
    ConversationScreenRouteContent(
        navigation = navigation,
        onShareAssetViaWire = onShareAssetViaWire,
        conversationInfoViewModel = conversationInfoViewModel,
        conversationBannerViewModel = conversationBannerViewModel,
        conversationCallViewModel = conversationCallViewModel,
        conversationMessagesViewModel = conversationMessagesViewModel,
        messageComposerViewModel = messageComposerViewModel,
        sendMessageViewModel = sendMessageViewModel,
        conversationMigrationViewModel = conversationMigrationViewModel,
        messageDraftViewModel = messageDraftViewModel,
        messageAttachmentsViewModel = messageAttachmentsViewModel,
    )
}
