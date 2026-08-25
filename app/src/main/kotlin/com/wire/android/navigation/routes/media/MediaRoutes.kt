/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.media

import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class MediaConversationId(val value: String, val domain: String) {
    init {
        require(value.isNotBlank())
        require(domain.isNotBlank())
    }
}

@Serializable
data class ConversationMediaRoute(
    override val sessionId: WireSessionId,
    val conversationId: MediaConversationId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/conversation_media_screen" }
}

@Serializable
data class ImagesPreviewRoute(
    override val sessionId: WireSessionId,
    val conversationId: MediaConversationId,
    val conversationName: String,
    val assetUris: List<String>,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/images_preview_screen" }
}

@Serializable
data class MediaGalleryRoute(
    override val sessionId: WireSessionId,
    val conversationId: MediaConversationId,
    val messageId: String,
    val isSelfAsset: Boolean,
    val isEphemeral: Boolean,
    val messageOptionsEnabled: Boolean,
    val cellAssetId: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/media_gallery_screen" }
}

@Serializable
data class VideoPlayerRoute(
    override val sessionId: WireSessionId,
    val localPath: String?,
    val contentUrl: String?,
    val fileName: String?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/video_player_screen" }
}

@Serializable
data class MessageDetailsRoute(
    override val sessionId: WireSessionId,
    val conversationId: MediaConversationId,
    val messageId: String,
    val isSelfMessage: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/message_details_screen" }
}

/**
 * Explicit logged-out share entry. Its type always resolves to the authentication Metro graph.
 */
@Serializable
data class LoggedOutImportMediaRoute(
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "app/import_media_screen" }
}

/**
 * Explicit authenticated share entry. It can only resolve to one concrete session Metro graph.
 */
@Serializable
data class AuthenticatedImportMediaRoute(
    override val sessionId: WireSessionId,
    val internalAssetUriStrings: List<String> = emptyList(),
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId = LoggedOutImportMediaRoute.ROUTE_ID
}

@Serializable
data class MediaAssetDto(
    val key: String,
    val mimeType: String,
    val dataPath: String,
    val dataSize: Long,
    val fileName: String,
    val assetType: String,
    val audioWavesMask: List<Int>? = null,
)

@Serializable
data class ImagesPreviewResult(val assets: List<MediaAssetDto>)

internal val ImagesPreviewResultContract = WireNavResultContract<ImagesPreviewResult>(
    WireNavResultContractId("media.images-preview")
)

@Serializable
enum class MediaGalleryResultAction { REPLY, REACT, DETAIL }

@Serializable
data class MediaGalleryResult(
    val messageId: String,
    val emoji: String? = null,
    val isSelfAsset: Boolean,
    val action: MediaGalleryResultAction,
    val cellAssetId: String? = null,
)

internal val MediaGalleryResultContract = WireNavResultContract<MediaGalleryResult>(
    WireNavResultContractId("media.gallery")
)
