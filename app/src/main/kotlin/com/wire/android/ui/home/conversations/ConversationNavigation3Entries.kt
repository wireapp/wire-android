/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui.home.conversations

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.feature.sketch.navigation.DrawingCanvasNavigation3ResultType
import com.wire.android.feature.sketch.navigation.DrawingCanvasRoute
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.routes.media.ImagesPreviewNavigation3ResultType
import com.wire.android.navigation.routes.media.AuthenticatedImportMediaRoute
import com.wire.android.navigation.routes.media.ImagesPreviewResult
import com.wire.android.navigation.routes.media.ImagesPreviewRoute
import com.wire.android.navigation.routes.media.MediaConversationId
import com.wire.android.navigation.routes.media.MediaGalleryNavigation3ResultType
import com.wire.android.navigation.routes.media.MediaGalleryResult
import com.wire.android.navigation.routes.media.MediaGalleryResultAction
import com.wire.android.navigation.routes.media.MediaGalleryRoute
import com.wire.android.navigation.routes.media.PdfViewerRoute
import com.wire.android.navigation.routes.media.VideoPlayerRoute
import com.wire.android.navigation.routes.media.MessageDetailsRoute
import com.wire.android.navigation.routes.media.toLegacy
import com.wire.android.ui.calling.conversationCallViewModel
import com.wire.android.ui.home.conversations.details.ConversationDetailsId
import com.wire.android.ui.home.conversations.details.GroupConversationActionType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsRoute
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.ui.userprofile.self.SelfUserProfileRoute
import com.wire.android.ui.userprofile.service.ServiceDetailsNavArgs
import com.wire.android.ui.userprofile.service.toServiceDetailsRoute
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand

internal val ConversationCompletionNavigation3ResultType = WireNavigation3ResultType(
    ConversationCompletionResultContract,
    ConversationCompletionResult.serializer(),
)

/**
 * Host actions for the Navigation 3 conversation entry and its result lifecycle.
 *
 * Unlike reusable screen action bundles, this contract intentionally includes `Navigation3` in
 * its name because it completes a typed Navigation 3 result and closes the owning entry.
 */
internal interface ConversationEntryNavigation3Actions {
    fun exitConversation()
    fun completeConversation(result: ConversationCompletionResult)
}

internal object ConversationNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(ConversationCompletionNavigation3ResultType)

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: ConversationEntryNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(conversationNavigation3Entries(runtime, actions))
}

internal fun conversationNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: ConversationEntryNavigation3Actions,
): WireEntryProviderInstaller = {
    // ConversationScreen had no destination override and inherited WireRoot's horizontal motion.
    wireEntry<ConversationRoute>(presentation = WireEntryPresentation.Slide) { route ->
        ConversationNavigation3Entry(route, runtime, actions)
    }
}

@Composable
private fun ConversationNavigation3Entry(
    route: ConversationRoute,
    runtime: WireNavigation3Runtime,
    actions: ConversationEntryNavigation3Actions,
) {
    val viewModelArgs = remember(route) { route.toViewModelArgs() }
    var groupRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var imagesRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var galleryRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var drawingRequestId by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    // Callbacks are intentionally not persisted. ConversationScreen re-registers them during every
    // composition, before this entry's LaunchedEffect consumes a restored request.
    val resultHandlers = remember { ConversationRouteResultHandlers() }

    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(currentEntryId, groupRequestId, imagesRequestId, galleryRequestId, drawingRequestId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        groupRequestId?.let(::WireNavResultRequestId)?.let { requestId ->
            runtime.consumeResult(requestId, ConversationCompletionNavigation3ResultType)?.let { result ->
                resultHandlers.groupDetails((result as? WireNavResult.Value)?.value?.toLegacy())
                groupRequestId = null
            }
        }
        imagesRequestId?.let(::WireNavResultRequestId)?.let { requestId ->
            runtime.consumeResult(requestId, ImagesPreviewNavigation3ResultType)?.let { result ->
                resultHandlers.imagesPreview((result as? WireNavResult.Value)?.value?.toLegacy())
                imagesRequestId = null
            }
        }
        galleryRequestId?.let(::WireNavResultRequestId)?.let { requestId ->
            runtime.consumeResult(requestId, MediaGalleryNavigation3ResultType)?.let { result ->
                resultHandlers.mediaGallery((result as? WireNavResult.Value)?.value?.toLegacy())
                galleryRequestId = null
            }
        }
        drawingRequestId?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, DrawingCanvasNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    resultHandlers.drawingCanvas(result.value.uri?.let(Uri::parse))
                    drawingRequestId = null
                }
                WireNavResult.Canceled -> {
                    resultHandlers.drawingCanvas(null)
                    drawingRequestId = null
                }
                null -> Unit
            }
        }
    }

    val navigation = object : ConversationRouteScreenNavigation {
        override fun goBack() {
            if (!runtime.navigator.goBack()) actions.exitConversation()
        }

        override fun replaceConversation(conversationId: ConversationId) {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = ConversationRoute(
                        sessionId = route.sessionId,
                        conversationId = conversationId.toConversationRouteId(),
                    ),
                    backStackMode = WireBackStackMode.REMOVE_CURRENT,
                )
            )
        }

        override fun openSelfUserProfile() {
            runtime.navigator.navigate(WireNavigationCommand(SelfUserProfileRoute(route.sessionId)))
        }

        override fun openOtherUserProfile(userId: UserId, conversationId: ConversationId?) {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    OtherUserProfileRoute(
                        sessionId = route.sessionId,
                        targetUserId = userId.toUserProfileId(),
                        groupConversationId = conversationId?.toUserProfileId(),
                    )
                )
            )
        }

        override fun openServiceDetails(args: ServiceDetailsNavArgs) {
            runtime.navigator.navigate(
                WireNavigationCommand(args.toServiceDetailsRoute(route.sessionId))
            )
        }

        override fun openMessageDetails(
            conversationId: ConversationId,
            messageId: String,
            isSelfMessage: Boolean,
        ) {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    MessageDetailsRoute(
                        route.sessionId,
                        conversationId.toMediaId(),
                        messageId,
                        isSelfMessage,
                    )
                )
            )
        }

        override fun openGroupDetails(conversationId: ConversationId) {
            groupRequestId = runtime.navigateForResult(
                GroupConversationDetailsRoute(
                    route.sessionId,
                    conversationId.toDetailsId(),
                ),
                ConversationCompletionNavigation3ResultType,
            )?.value
        }

        override fun openImagesPreview(
            conversationId: ConversationId,
            conversationName: String,
            assetUris: List<Uri>,
        ) {
            imagesRequestId = runtime.navigateForResult(
                ImagesPreviewRoute(
                    route.sessionId,
                    conversationId.toMediaId(),
                    conversationName,
                    assetUris.map { it.toString() },
                ),
                ImagesPreviewNavigation3ResultType,
            )?.value
        }

        override fun openMediaGallery(
            conversationId: ConversationId,
            messageId: String,
            isSelfAsset: Boolean,
            isEphemeral: Boolean,
            messageOptionsEnabled: Boolean,
            cellAssetId: String?,
        ) {
            galleryRequestId = runtime.navigateForResult(
                MediaGalleryRoute(
                    route.sessionId,
                    conversationId.toMediaId(),
                    messageId,
                    isSelfAsset,
                    isEphemeral,
                    messageOptionsEnabled,
                    cellAssetId,
                ),
                MediaGalleryNavigation3ResultType,
            )?.value
        }

        override fun openVideoPlayer(localPath: String?, contentUrl: String?, fileName: String?) {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    VideoPlayerRoute(route.sessionId, localPath, contentUrl, fileName)
                )
            )
        }

        override fun openPdfViewer(localPath: String?, contentUrl: String?, fileName: String?) {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    PdfViewerRoute(route.sessionId, localPath, contentUrl, fileName)
                )
            )
        }

        override fun openDrawingCanvas(conversationName: String, tempWritableUri: Uri?) {
            drawingRequestId = runtime.navigateForResult(
                DrawingCanvasRoute(
                    route.sessionId,
                    conversationName,
                    tempWritableUri?.toString(),
                ),
                DrawingCanvasNavigation3ResultType,
            )?.value
        }

        override fun onGroupDetailsResult(handler: (GroupConversationDetailsNavBackArgs?) -> Unit) {
            resultHandlers.groupDetails = handler
        }

        override fun onImagesPreviewResult(handler: (ImagesPreviewNavBackArgs?) -> Unit) {
            resultHandlers.imagesPreview = handler
        }

        override fun onMediaGalleryResult(handler: (MediaGalleryNavBackArgs?) -> Unit) {
            resultHandlers.mediaGallery = handler
        }

        override fun onDrawingCanvasResult(handler: (Uri?) -> Unit) {
            resultHandlers.drawingCanvas = handler
        }

        override fun completeConversation(result: GroupConversationDetailsNavBackArgs) {
            val typed = result.toNavigation3()
            if (!runtime.completeCurrentAndPop(
                    ConversationCompletionNavigation3ResultType,
                    WireNavResult.Value(typed),
                )
            ) {
                actions.completeConversation(typed)
            }
        }
    }

    ConversationRouteScreen(
        navigation = navigation,
        onShareAssetViaWire = { uri ->
            runtime.navigator.navigate(
                WireNavigationCommand(
                    AuthenticatedImportMediaRoute(route.sessionId, listOf(uri.toString()))
                )
            )
        },
        conversationInfoViewModel = conversationInfoViewModel(viewModelArgs),
        conversationBannerViewModel = conversationBannerViewModel(viewModelArgs),
        conversationCallViewModel = conversationCallViewModel(viewModelArgs),
        conversationMessagesViewModel = conversationMessagesViewModel(viewModelArgs),
        messageComposerViewModel = messageComposerViewModel(viewModelArgs),
        sendMessageViewModel = sendMessageViewModel(viewModelArgs),
        conversationMigrationViewModel = conversationMigrationViewModel(viewModelArgs),
        messageDraftViewModel = messageDraftViewModel(viewModelArgs),
        messageAttachmentsViewModel = messageAttachmentsViewModel(viewModelArgs),
    )
}

private fun ConversationId.toMediaId() = MediaConversationId(value, domain)
private fun ConversationId.toDetailsId() = ConversationDetailsId(value, domain)
private fun UserId.toUserProfileId() = UserProfileQualifiedId(value, domain)

private fun ConversationCompletionResult.toLegacy() = GroupConversationDetailsNavBackArgs(
    groupConversationActionType = when (action) {
        ConversationCompletionAction.LEAVE_GROUP -> GroupConversationActionType.LEAVE_GROUP
        ConversationCompletionAction.DELETE_GROUP -> GroupConversationActionType.DELETE_GROUP
    },
    conversationName = conversationName,
)

internal fun GroupConversationDetailsNavBackArgs.toNavigation3() = ConversationCompletionResult(
    action = when (groupConversationActionType) {
        GroupConversationActionType.LEAVE_GROUP -> ConversationCompletionAction.LEAVE_GROUP
        GroupConversationActionType.DELETE_GROUP -> ConversationCompletionAction.DELETE_GROUP
    },
    conversationName = conversationName,
)

private fun ImagesPreviewResult.toLegacy() =
    ImagesPreviewNavBackArgs(assets.map { it.toLegacy() })

private fun MediaGalleryResult.toLegacy() = MediaGalleryNavBackArgs(
    messageId = messageId,
    emoji = emoji,
    isSelfAsset = isSelfAsset,
    mediaGalleryActionType = when (action) {
        MediaGalleryResultAction.REPLY -> MediaGalleryActionType.REPLY
        MediaGalleryResultAction.REACT -> MediaGalleryActionType.REACT
        MediaGalleryResultAction.DETAIL -> MediaGalleryActionType.DETAIL
    },
    cellAssetId = cellAssetId,
)
