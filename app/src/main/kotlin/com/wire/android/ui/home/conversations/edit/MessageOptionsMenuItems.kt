/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.edit

import androidx.compose.runtime.Composable

@Composable
fun messageOptionsMenuItems(
    isAssetMessage: Boolean,
    isEphemeral: Boolean,
    isUploading: Boolean,
    isOpenable: Boolean,
    isComposite: Boolean,
    isEditable: Boolean,
    isCopyable: Boolean,
    showReplyInThreadOption: Boolean,
    showLegacyReplyOption: Boolean,
    ownReactions: Set<String>,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReactionClick: (reactionEmoji: String) -> Unit,
    onDetailsClick: () -> Unit,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareAssetClick: () -> Unit,
    onDownloadAssetClick: () -> Unit,
    onOpenAssetClick: () -> Unit,
): List<@Composable () -> Unit> {
    return if (isAssetMessage) {
        assetMessageOptionsMenuItems(
            ownReactions = ownReactions,
            isEphemeral = isEphemeral,
            isUploading = isUploading,
            isOpenable = isOpenable,
            onDeleteClick = onDeleteClick,
            onDetailsClick = onDetailsClick,
            onShareAsset = onShareAssetClick,
            onDownloadAsset = onDownloadAssetClick,
            onReplyClick = onReplyClick,
            onReactionClick = onReactionClick,
            onOpenAsset = onOpenAssetClick,
            showReplyInThreadOption = showReplyInThreadOption,
            showLegacyReplyOption = showLegacyReplyOption,
        )
    } else {
        textMessageEditMenuItems(
            ownReactions = ownReactions,
            isEphemeral = isEphemeral,
            isUploading = isUploading,
            isComposite = isComposite,
            isEditable = isEditable,
            isCopyable = isCopyable,
            onDeleteClick = onDeleteClick,
            onDetailsClick = onDetailsClick,
            onReactionClick = onReactionClick,
            onEditClick = onEditClick,
            onCopyClick = onCopyClick,
            onReplyClick = onReplyClick,
            showReplyInThreadOption = showReplyInThreadOption,
            showLegacyReplyOption = showLegacyReplyOption,
        )
    }
}
