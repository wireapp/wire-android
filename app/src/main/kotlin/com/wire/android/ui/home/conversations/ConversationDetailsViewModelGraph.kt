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
import com.wire.android.di.metro.sessionKeyedMetroViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel

@Composable
fun groupConversationDetailsViewModel(): GroupConversationDetailsViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun groupConversationParticipantsViewModel(): GroupConversationParticipantsViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun editConversationMetadataViewModel(): EditConversationMetadataViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun editSelfDeletingMessagesViewModel(): EditSelfDeletingMessagesViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun updateChannelAccessViewModel(): UpdateChannelAccessViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun updateAppsAccessViewModel(): UpdateAppsAccessViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun editGuestAccessViewModel(): EditGuestAccessViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun createPasswordGuestLinkViewModel(): CreatePasswordGuestLinkViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun checkAssetRestrictionsViewModel(): CheckAssetRestrictionsViewModel =
    sessionKeyedMetroViewModel()
