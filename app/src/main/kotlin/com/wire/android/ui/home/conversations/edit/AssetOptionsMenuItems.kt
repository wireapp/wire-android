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
import com.wire.android.ui.edit.DeleteItemMenuOption
import com.wire.android.ui.edit.DownloadAssetExternallyOption
import com.wire.android.ui.edit.MessageDetailsMenuOption
import com.wire.android.ui.edit.OpenAssetExternallyOption
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.edit.ReplyInThreadMessageOption
import com.wire.android.ui.edit.ReplyMessageOption
import com.wire.android.ui.edit.ShareAssetMenuOption

// menu items with both asset options enabled (like share, download, etc.) and message options enabled (like reply, reaction, etc.)
@Composable
fun assetMessageOptionsMenuItems(
    isEphemeral: Boolean,
    ownReactions: Set<String>,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onShareAsset: () -> Unit,
    onDownloadAsset: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (emoji: String) -> Unit,
    isOpenable: Boolean = false,
    onOpenAsset: () -> Unit = {},
    isUploading: Boolean = false,
    showReplyInThreadOption: Boolean = false,
    showLegacyReplyOption: Boolean = false,
): List<@Composable () -> Unit> {
    return buildList {
        when {
            isUploading -> {
                add { DeleteItemMenuOption(onDeleteClick) }
            }

            isEphemeral -> {
                add { MessageDetailsMenuOption(onDetailsClick) }
                add { DownloadAssetExternallyOption(onDownloadAsset) }
                if (isOpenable) add { OpenAssetExternallyOption(onOpenAsset) }
                add { DeleteItemMenuOption(onDeleteClick) }
            }

            else -> {
                add { ReactionOption(ownReactions, onReactionClick) }
                add { MessageDetailsMenuOption(onDetailsClick) }
                if (showReplyInThreadOption) {
                    add { ReplyInThreadMessageOption(onReplyClick) }
                } else if (showLegacyReplyOption) {
                    add { ReplyMessageOption(onReplyClick) }
                }
                add { DownloadAssetExternallyOption(onDownloadAsset) }
                add { ShareAssetMenuOption(onShareAsset) }
                if (isOpenable) add { OpenAssetExternallyOption(onOpenAsset) }
                add { DeleteItemMenuOption(onDeleteClick) }
            }
        }
    }
}

// menu items with only asset options enabled (like share, download, etc.)
@Composable
fun assetOptionsMenuItems(
    isEphemeral: Boolean,
    onDeleteClick: () -> Unit,
    onShareAsset: () -> Unit,
    onDownloadAsset: () -> Unit,
    isOpenable: Boolean = false,
    onOpenAsset: () -> Unit = {},
    isUploading: Boolean = false,
): List<@Composable () -> Unit> = buildList {
    if (!isUploading) {
        add { DownloadAssetExternallyOption(onDownloadAsset) }
        if (!isEphemeral) add { ShareAssetMenuOption(onShareAsset) }
        if (isOpenable) add { OpenAssetExternallyOption(onOpenAsset) }
    }
    add { DeleteItemMenuOption(onDeleteClick) }
}
