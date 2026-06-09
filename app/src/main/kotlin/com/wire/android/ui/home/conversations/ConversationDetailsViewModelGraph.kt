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
import com.wire.android.di.metro.scopedMetroViewModel
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
    scopedMetroViewModel()

@Composable
fun groupConversationParticipantsViewModel(): GroupConversationParticipantsViewModel =
    scopedMetroViewModel()

@Composable
fun editConversationMetadataViewModel(): EditConversationMetadataViewModel =
    scopedMetroViewModel()

@Composable
fun editSelfDeletingMessagesViewModel(): EditSelfDeletingMessagesViewModel =
    scopedMetroViewModel()

@Composable
fun updateChannelAccessViewModel(): UpdateChannelAccessViewModel =
    scopedMetroViewModel()

@Composable
fun updateAppsAccessViewModel(): UpdateAppsAccessViewModel =
    scopedMetroViewModel()

@Composable
fun editGuestAccessViewModel(): EditGuestAccessViewModel =
    scopedMetroViewModel()

@Composable
fun createPasswordGuestLinkViewModel(): CreatePasswordGuestLinkViewModel =
    scopedMetroViewModel()

@Composable
fun checkAssetRestrictionsViewModel(): CheckAssetRestrictionsViewModel =
    scopedMetroViewModel()
