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
@file:Suppress("TooManyFunctions", "MatchingDeclarationName")

package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessNavArgs
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesNavArgs
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationNameNavArgs
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessNavArgs
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup

@WireAssistedViewModelFactoryGroup
object ConversationDetailsManualViewModelFactoryGroup

@Composable
fun editConversationMetadataViewModel(): EditConversationMetadataViewModel =
    wireMetroViewModel()

@Composable
fun editConversationMetadataViewModel(args: EditConversationNameNavArgs): EditConversationMetadataViewModel =
    conversationDetailsAssistedViewModel { editConversationMetadataViewModel(args) }

@Composable
fun editSelfDeletingMessagesViewModel(): EditSelfDeletingMessagesViewModel =
    wireMetroViewModel()

@Composable
fun editSelfDeletingMessagesViewModel(
    args: EditSelfDeletingMessagesNavArgs,
): EditSelfDeletingMessagesViewModel =
    conversationDetailsAssistedViewModel { editSelfDeletingMessagesViewModel(args) }

@Composable
fun updateAppsAccessViewModel(): UpdateAppsAccessViewModel =
    wireMetroViewModel()

@Composable
fun updateAppsAccessViewModel(args: UpdateAppsAccessNavArgs): UpdateAppsAccessViewModel =
    conversationDetailsAssistedViewModel { updateAppsAccessViewModel(args) }

@Composable
fun editGuestAccessViewModel(): EditGuestAccessViewModel =
    wireMetroViewModel()

@Composable
fun editGuestAccessViewModel(args: EditGuestAccessNavArgs): EditGuestAccessViewModel =
    conversationDetailsAssistedViewModel { editGuestAccessViewModel(args) }

@Composable
fun checkAssetRestrictionsViewModel(): CheckAssetRestrictionsViewModel =
    wireMetroViewModel()

@Composable
private inline fun <reified VM> conversationDetailsAssistedViewModel(
    crossinline create: ConversationDetailsManualViewModelFactory.() -> VM,
): VM where VM : ViewModel =
    wireAssistedMetroViewModel<VM, ConversationDetailsManualViewModelFactory>(
        create = { _ -> create() },
    )
