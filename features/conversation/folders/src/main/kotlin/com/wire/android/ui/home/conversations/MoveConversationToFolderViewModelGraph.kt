/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import com.wire.android.di.ConversationFoldersViewModelScopedPreviews as ViewModelScopedPreviews
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup
import com.wire.android.di.metro.wireAssistedMetroViewModelAs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVM
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl

@WireAssistedViewModelFactoryGroup
object MoveConversationToFolderManualViewModelFactoryGroup

@Composable
fun moveConversationToFolderViewModel(
    args: MoveConversationToFolderArgs,
): MoveConversationToFolderVM =
    wireAssistedMetroViewModelAs<
        MoveConversationToFolderVMImpl,
        MoveConversationToFolderVM,
        MoveConversationToFolderManualViewModelFactory,
        >(
        instanceKey = "move_conversation_to_folder_${args.conversationId}_${args.currentFolderId}",
        previewProvider = ViewModelScopedPreviews,
    ) { _ ->
        moveConversationToFolderViewModel(args)
    }
