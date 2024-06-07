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
import com.wire.android.ui.edit.ReplyMessageOption

@Composable
fun assetEditMenuItems(
    isEphemeral: Boolean,
    messageOptionsEnabled: Boolean,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onShareAsset: () -> Unit,
    onDownloadAsset: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    isUploading: Boolean = false,
    onOpenAsset: (() -> Unit)? = null
): List<@Composable () -> Unit> {
    return buildList {
        when {
            isUploading -> {
                add { DeleteItemMenuOption(onDeleteClick) }
            }

            isEphemeral -> {
                if (messageOptionsEnabled) {
                    add { MessageDetailsMenuOption(onDetailsClick) }
                }
                add { DownloadAssetExternallyOption(onDownloadAsset) }
                add { DeleteItemMenuOption(onDeleteClick) }
            }

            !messageOptionsEnabled -> {
                add { DownloadAssetExternallyOption(onDownloadAsset) }
                add { ShareAssetMenuOption(onShareAsset) }
                add { DeleteItemMenuOption(onDeleteClick) }
            }

            else -> {
                add { ReactionOption(onReactionClick) }
                add { MessageDetailsMenuOption(onDetailsClick) }
                add { ReplyMessageOption(onReplyClick) }
                add { DownloadAssetExternallyOption(onDownloadAsset) }
                add { ShareAssetMenuOption(onShareAsset) }
                if (onOpenAsset != null) add { OpenAssetExternallyOption(onOpenAsset) }
                add { DeleteItemMenuOption(onDeleteClick) }
            }
        }
    }
}
