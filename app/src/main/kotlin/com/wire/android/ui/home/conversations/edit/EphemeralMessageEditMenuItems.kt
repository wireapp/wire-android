/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import com.wire.android.ui.edit.DownloadAssetExternallyOption
import com.wire.android.ui.edit.MessageDetailsMenuOption
import com.wire.android.ui.edit.OpenAssetExternallyOption
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent

@Composable
fun EphemeralMessageEditMenuItems(
    message: UIMessage.Regular,
    onDetailsClick: () -> Unit,
    onDownloadAsset: () -> Unit,
    onOpenAsset: () -> Unit,
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    val isAvailable = message.isAvailable
    val isAssetMessage = message.messageContent is UIMessageContent.AssetMessage
            || message.messageContent is UIMessageContent.ImageMessage
            || message.messageContent is UIMessageContent.AudioAssetMessage
    val isGenericAsset = message.messageContent is UIMessageContent.AssetMessage

    return buildList {
        if (isAvailable) {
            add { MessageDetailsMenuOption(onDetailsClick) }
            if (isAssetMessage) add { DownloadAssetExternallyOption(onDownloadAsset) }
            if (isGenericAsset) add { OpenAssetExternallyOption(onOpenAsset) }
            add { DeleteItemMenuOption(onDeleteMessage) }
        }
    }
}

