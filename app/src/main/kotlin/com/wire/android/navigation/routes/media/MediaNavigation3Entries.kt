/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.navigation.routes.media

import android.net.Uri
import androidx.compose.runtime.Composable
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.videoplayer.VideoPlayer
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.checkAssetRestrictionsViewModel
import com.wire.android.ui.home.conversations.conversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.conversationMessagesViewModel
import com.wire.android.ui.home.conversations.imagesPreviewViewModel
import com.wire.android.ui.home.conversations.media.ConversationMediaRouteScreen
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewRouteScreen
import com.wire.android.ui.home.conversations.messageDetailsViewModel
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsRouteScreen
import com.wire.android.ui.home.conversations.mediaGalleryViewModel
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.gallery.MediaGalleryRouteScreen
import com.wire.android.ui.home.featureFlagNotificationViewModel
import com.wire.android.ui.importMediaAuthenticatedViewModel
import com.wire.android.ui.sharing.ImportMediaAuthenticatedContent
import com.wire.android.ui.sharing.ImportMediaLoggedOutContent
import com.wire.android.ui.sharing.ImportMediaLoadingContent
import com.wire.navigation.WireNavResult

internal val ImagesPreviewNavigation3ResultType = WireNavigation3ResultType(
    ImagesPreviewResultContract,
    ImagesPreviewResult.serializer(),
)
internal val MediaGalleryNavigation3ResultType = WireNavigation3ResultType(
    MediaGalleryResultContract,
    MediaGalleryResult.serializer(),
)

internal interface MediaNavigation3Actions {
    fun finishShare()
    fun openAuthentication()
    fun openConversationFromShare(
        conversationId: MediaConversationId,
        assets: List<MediaAssetDto>,
        text: String?,
    )
    fun openPublicLink(assetId: String, fileName: String, publicLinkId: String?)
}

internal object MediaNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(ImagesPreviewNavigation3ResultType, MediaGalleryNavigation3ResultType)

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: MediaNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(mediaNavigation3Entries(runtime, actions))
}

@Suppress("LongMethod")
internal fun mediaNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: MediaNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<ConversationMediaRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        ConversationMediaRouteScreen(
            conversationAssetMessagesViewModel(route.toViewModelArgs()),
            conversationMessagesViewModel(ConversationNavArgs(route.conversationId.toQualifiedId())),
            runtime.navigator::goBack,
            onShareAssetViaWire = { uri ->
                runtime.navigator.navigate(
                    com.wire.navigation.WireNavigationCommand(
                        AuthenticatedImportMediaRoute(route.sessionId, listOf(uri.toString()))
                    )
                )
            },
        ) { conversationId, messageId, selfAsset, cellAssetId ->
            runtime.navigator.navigate(
                com.wire.navigation.WireNavigationCommand(
                    MediaGalleryRoute(
                        route.sessionId,
                        conversationId.toMediaConversationId(),
                        messageId,
                        selfAsset,
                        false,
                        false,
                        cellAssetId,
                    )
                )
            )
        }
    }
    wireEntry<ImagesPreviewRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        ImagesPreviewRouteScreen(
            imagesPreviewViewModel(route.toViewModelArgs()),
            checkAssetRestrictionsViewModel(),
            runtime.navigator::goBack,
        ) { assets ->
            if (!runtime.completeCurrentAndPop(
                    ImagesPreviewNavigation3ResultType,
                    WireNavResult.Value(ImagesPreviewResult(assets.map { it.toNavigationDto() })),
                )
            ) {
                runtime.navigator.goBack()
            }
        }
    }
    wireEntry<MediaGalleryRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        MediaGalleryNavigation3Entry(route, runtime, actions)
    }
    wireEntry<VideoPlayerRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        VideoPlayer(
            localPath = route.localPath,
            contentUrl = route.contentUrl,
            fileName = route.fileName,
            onNavigateBack = runtime.navigator::goBack,
        )
    }
    wireEntry<MessageDetailsRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        MessageDetailsRouteScreen(messageDetailsViewModel(route.toViewModelArgs()), runtime.navigator::goBack)
    }
    wireEntry<LoggedOutImportMediaRoute>(presentation = WireEntryPresentation.PopUp) {
        ImportMediaLoggedOutContent(
            FeatureFlagState.FileSharingState.NoUser,
            actions::finishShare,
            actions::openAuthentication,
        )
    }
    wireEntry<AuthenticatedImportMediaRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        AuthenticatedImportMediaNavigation3Entry(route, actions)
    }
}

@Composable
private fun AuthenticatedImportMediaNavigation3Entry(
    route: AuthenticatedImportMediaRoute,
    actions: MediaNavigation3Actions,
) {
    when (val state = featureFlagNotificationViewModel().featureFlagState.isFileSharingState) {
        FeatureFlagState.FileSharingState.Loading -> ImportMediaLoadingContent(actions::finishShare)
        FeatureFlagState.FileSharingState.NoUser -> ImportMediaLoggedOutContent(
            state,
            actions::finishShare,
            actions::openAuthentication,
        )
        FeatureFlagState.FileSharingState.DisabledByTeam,
        FeatureFlagState.FileSharingState.AllowAll,
        is FeatureFlagState.FileSharingState.AllowSome,
        -> ImportMediaAuthenticatedContent(
            isRestrictedInTeam = state == FeatureFlagState.FileSharingState.DisabledByTeam,
            navigateBack = actions::finishShare,
            onConversationReady = { conversationId, assets, text ->
                actions.openConversationFromShare(
                    conversationId.toMediaConversationId(),
                    assets.map { it.toNavigationDto() },
                    text,
                )
            },
            internalAssetUris = route.internalAssetUriStrings.map(Uri::parse),
            importMediaViewModel = importMediaAuthenticatedViewModel(),
        )
    }
}

@Composable
private fun MediaGalleryNavigation3Entry(
    route: MediaGalleryRoute,
    runtime: WireNavigation3Runtime,
    actions: MediaNavigation3Actions,
) {
    MediaGalleryRouteScreen(
        mediaGalleryViewModel(route.toViewModelArgs()),
        runtime.navigator::goBack,
        onResult = { result ->
            val typed = result.toNavigationResult()
            if (!runtime.completeCurrentAndPop(
                    MediaGalleryNavigation3ResultType,
                    WireNavResult.Value(typed),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
        onOpenPublicLink = actions::openPublicLink,
        onShareAssetViaWire = { uri ->
            runtime.navigator.navigate(
                com.wire.navigation.WireNavigationCommand(
                    AuthenticatedImportMediaRoute(route.sessionId, listOf(uri.toString()))
                )
            )
        },
    )
}

private fun MediaGalleryNavBackArgs.toNavigationResult() = MediaGalleryResult(
    messageId,
    emoji,
    isSelfAsset,
    when (mediaGalleryActionType) {
        MediaGalleryActionType.REPLY -> MediaGalleryResultAction.REPLY
        MediaGalleryActionType.REACT -> MediaGalleryResultAction.REACT
        MediaGalleryActionType.DETAIL -> MediaGalleryResultAction.DETAIL
    },
    cellAssetId,
)
