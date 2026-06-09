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
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel

interface ConversationDetailsViewModelGraph : MetroViewModelGraph {
    val conversationDetailsViewModelFactory: ConversationDetailsViewModelFactory
}

@Composable
fun groupConversationDetailsViewModel(): GroupConversationDetailsViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, GroupConversationDetailsViewModel> {
        conversationDetailsViewModelFactory.groupConversationDetailsViewModel(it)
    }

@Composable
fun groupConversationParticipantsViewModel(): GroupConversationParticipantsViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, GroupConversationParticipantsViewModel> {
        conversationDetailsViewModelFactory.groupConversationParticipantsViewModel(it)
    }

@Composable
fun editConversationMetadataViewModel(): EditConversationMetadataViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, EditConversationMetadataViewModel> {
        conversationDetailsViewModelFactory.editConversationMetadataViewModel(it)
    }

@Composable
fun editSelfDeletingMessagesViewModel(): EditSelfDeletingMessagesViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, EditSelfDeletingMessagesViewModel> {
        conversationDetailsViewModelFactory.editSelfDeletingMessagesViewModel(it)
    }

@Composable
fun updateChannelAccessViewModel(): UpdateChannelAccessViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, UpdateChannelAccessViewModel> {
        conversationDetailsViewModelFactory.updateChannelAccessViewModel(it)
    }

@Composable
fun updateAppsAccessViewModel(): UpdateAppsAccessViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, UpdateAppsAccessViewModel> {
        conversationDetailsViewModelFactory.updateAppsAccessViewModel(it)
    }

@Composable
fun editGuestAccessViewModel(): EditGuestAccessViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, EditGuestAccessViewModel> {
        conversationDetailsViewModelFactory.editGuestAccessViewModel(it)
    }

@Composable
fun createPasswordGuestLinkViewModel(): CreatePasswordGuestLinkViewModel =
    metroSavedStateViewModel<ConversationDetailsViewModelGraph, CreatePasswordGuestLinkViewModel> {
        conversationDetailsViewModelFactory.createPasswordGuestLinkViewModel(it)
    }

@Composable
fun checkAssetRestrictionsViewModel(): CheckAssetRestrictionsViewModel =
    metroViewModel<ConversationDetailsViewModelGraph, CheckAssetRestrictionsViewModel> {
        conversationDetailsViewModelFactory.checkAssetRestrictionsViewModel()
    }
