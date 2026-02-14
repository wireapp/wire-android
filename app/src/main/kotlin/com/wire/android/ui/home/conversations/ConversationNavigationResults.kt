/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ramcosta.composedestinations.generated.app.destinations.GroupConversationDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ImagesPreviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.MediaGalleryScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.MessageDetailsScreenDestination
import com.ramcosta.composedestinations.result.NavResult.Canceled
import com.ramcosta.composedestinations.result.NavResult.Value
import com.ramcosta.composedestinations.result.OpenResultRecipient
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.appLogger
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("CyclomaticComplexMethod", "LongParameterList")
@Composable
internal fun ConversationNavigationResults(
    groupDetailsScreenResultRecipient:
    ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    imagePreviewScreenResultRecipient: ResultRecipient<ImagesPreviewScreenDestination, ImagesPreviewNavBackArgs>,
    drawingCanvasScreenResultRecipient: OpenResultRecipient<DrawingCanvasNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    navigator: Navigator,
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    getAndResetLastFullscreenMessage: (String) -> UIMessage.Regular?,
    toggleReaction: (messageId: String, emoji: String) -> Unit,
    trySendMessages: (List<MessageBundle>) -> Unit,
    trySendMessage: (MessageBundle) -> Unit,
    onConversationDeleted: () -> Unit,
    handleGroupDetailsResult: Boolean = true,
) {
    val coroutineScope = rememberCoroutineScope()

    if (handleGroupDetailsResult) {
        groupDetailsScreenResultRecipient.onNavResult { result ->
            when (result) {
                is Canceled -> {
                    appLogger.i("Error with receiving navigation back args from groupDetails in ConversationScreen")
                }

                is Value -> {
                    resultNavigator.setResult(result.value)
                    resultNavigator.navigateBack()
                    onConversationDeleted()
                }
            }
        }
    }

    mediaGalleryScreenResultRecipient.onNavResult { result ->
        when (result) {
            is Canceled -> {
                appLogger.i("Error with receiving navigation back args from mediaGallery in ConversationScreen")
            }

            is Value -> {
                when (result.value.mediaGalleryActionType) {
                    MediaGalleryActionType.REPLY -> {
                        getAndResetLastFullscreenMessage(result.value.messageId)?.let {
                            coroutineScope.launch {
                                withSmoothScreenLoad {
                                    messageComposerStateHolder.toReply(it)
                                }
                            }
                        }
                    }

                    MediaGalleryActionType.REACT -> {
                        result.value.emoji?.let { toggleReaction(result.value.messageId, it) }
                    }

                    MediaGalleryActionType.DETAIL -> {
                        getAndResetLastFullscreenMessage(result.value.messageId)?.let {
                            navigator.navigate(
                                NavigationCommand(
                                    MessageDetailsScreenDestination(
                                        conversationId,
                                        result.value.messageId,
                                        result.value.isSelfAsset,
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    imagePreviewScreenResultRecipient.onNavResult { result ->
        when (result) {
            Canceled -> Unit
            is Value -> {
                trySendMessages(
                    result.value.pendingBundles.map { assetBundle ->
                        ComposableMessageBundle.AttachmentPickedBundle(
                            conversationId = conversationId,
                            assetBundle = assetBundle,
                        )
                    }
                )
            }
        }
    }

    drawingCanvasScreenResultRecipient.onNavResult { result ->
        when (result) {
            Canceled -> Unit
            is Value -> {
                trySendMessage(
                    ComposableMessageBundle.UriPickedBundle(
                        conversationId = conversationId,
                        attachmentUri = UriAsset(result.value.uri),
                    )
                )
            }
        }
    }
}

private fun CoroutineScope.withSmoothScreenLoad(block: () -> Unit) = launch {
    val smoothAnimationDuration = 200.milliseconds
    delay(smoothAnimationDuration) // we wait a bit until the whole screen is loaded to show the animation properly
    block()
}
