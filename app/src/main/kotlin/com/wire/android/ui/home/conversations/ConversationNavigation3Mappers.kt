/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui.home.conversations

import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.QualifiedID
import okio.Path.Companion.toPath

internal fun ConversationRoute.toViewModelArgs() = ConversationNavArgs(
    conversationId = conversationId.toQualifiedId(),
    searchedMessageId = searchedMessageId,
    pendingBundles = pendingAssets
        .mapTo(ArrayList()) { it.toLegacy() }
        .takeUnless { it.isEmpty() },
    pendingTextBundle = pendingText,
)

internal fun ConversationRouteId.toQualifiedId() = QualifiedID(value, domain)
internal fun QualifiedID.toConversationRouteId() = ConversationRouteId(value, domain)

internal fun AssetBundle.toConversationPendingAsset() = ConversationPendingAsset(
    key = key,
    mimeType = mimeType,
    dataPath = dataPath.toString(),
    dataSize = dataSize,
    fileName = fileName,
    assetType = assetType.name,
    audioWavesMask = audioWavesMask,
)

internal fun ConversationPendingAsset.toLegacy() = AssetBundle(
    key = key,
    mimeType = mimeType,
    dataPath = dataPath.toPath(),
    dataSize = dataSize,
    fileName = fileName,
    assetType = AttachmentType.valueOf(assetType),
    audioWavesMask = audioWavesMask,
)
