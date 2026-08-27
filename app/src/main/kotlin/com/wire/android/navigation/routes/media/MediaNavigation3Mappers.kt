/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.navigation.routes.media

import com.wire.android.ui.home.conversations.media.ConversationMediaNavArgs
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavArgs
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsNavArgs
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.content.external.ExternalContentImportRequest
import com.wire.content.external.ExternalContentReference
import com.wire.android.ui.home.gallery.MediaGalleryNavArgs
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.QualifiedID
import okio.Path.Companion.toPath

internal fun MediaConversationId.toQualifiedId() = QualifiedID(value, domain)
internal fun QualifiedID.toMediaConversationId() = MediaConversationId(value, domain)

internal fun ConversationMediaRoute.toViewModelArgs() = ConversationMediaNavArgs(conversationId.toQualifiedId())
internal fun ImagesPreviewRoute.toViewModelArgs() = ImagesPreviewNavArgs(
    conversationId.toQualifiedId(),
    conversationName,
    ArrayList(assetUris.map { ExternalContentImportRequest(ExternalContentReference(it)) }),
)
internal fun MediaGalleryRoute.toViewModelArgs() = MediaGalleryNavArgs(
    conversationId.toQualifiedId(),
    messageId,
    isSelfAsset,
    isEphemeral,
    messageOptionsEnabled,
    cellAssetId,
)
internal fun MessageDetailsRoute.toViewModelArgs() =
    MessageDetailsNavArgs(conversationId.toQualifiedId(), messageId, isSelfMessage)

internal fun AssetBundle.toNavigationDto() = MediaAssetDto(
    key,
    mimeType,
    dataPath.toString(),
    dataSize,
    fileName,
    assetType.name,
    audioWavesMask
)
internal fun MediaAssetDto.toLegacy() = AssetBundle(
    key,
    mimeType,
    dataPath.toPath(),
    dataSize,
    fileName,
    AttachmentType.valueOf(assetType),
    audioWavesMask
)
