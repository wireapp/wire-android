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
package com.wire.android.ui.home.conversations.media.preview

import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class ImagesPreviewState(
    val conversationId: ConversationId,
    val conversationName: String,
    val assetBundleList: PersistentList<ImportedMediaAsset> = persistentListOf(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false
)
