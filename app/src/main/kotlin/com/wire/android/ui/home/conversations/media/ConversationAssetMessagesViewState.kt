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

import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.wire.android.ui.home.conversations.usecase.UIImageAssetPagingItem
import com.wire.android.ui.home.conversations.usecase.UIPagingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Stable
data class ConversationAssetMessagesViewState(
    val imageMessages: Flow<PagingData<UIImageAssetPagingItem>> = emptyFlow(),
    val assetMessages: Flow<PagingData<UIPagingItem>> = emptyFlow()
)
