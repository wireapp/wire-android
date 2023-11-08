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
 *
 *
 */

package com.wire.android.ui.home.conversations.media

import androidx.paging.PagingData
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.home.conversations.messages.DownloadedAssetDialogVisibilityState
import com.wire.android.ui.home.conversations.model.UIMessageContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class ConversationAssetMessagesViewState(
    val messages: Flow<PagingData<UIMessageContent>> = emptyFlow(),
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden,
    val audioMessagesState: Map<String, AudioState> = emptyMap()
)
