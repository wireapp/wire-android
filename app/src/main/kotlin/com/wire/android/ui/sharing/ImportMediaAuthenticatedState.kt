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
package com.wire.android.ui.sharing

import androidx.compose.runtime.Stable
import androidx.paging.PagingData
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Stable
data class ImportMediaAuthenticatedState(
    val importedAssets: PersistentList<ImportedMediaAsset> = persistentListOf(),
    val importedText: String? = null,
    val isImporting: Boolean = false,
    val conversations: Flow<PagingData<ConversationItemType>> = emptyFlow(),
    val selectedConversationItem: List<ConversationId> = persistentListOf(),
    val selfDeletingTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(null)
) {
    @Stable
    fun isImportingData() {
        importedText?.isNotEmpty() == true || importedAssets.isNotEmpty()
    }
}
