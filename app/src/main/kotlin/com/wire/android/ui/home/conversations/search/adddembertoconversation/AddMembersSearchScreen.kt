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
package com.wire.android.ui.home.conversations.search.adddembertoconversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Contact
import com.wire.android.model.ItemActionType
import com.wire.android.search.SearchUsersAndAppsScreen
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.newconversation.common.ContinueButton

@Composable
internal fun AddMembersSearchRouteScreen(
    viewModel: AddMembersToConversationViewModel,
    navArgs: AddMembersSearchNavArgs,
    onNavigateBack: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onOpenService: (Contact) -> Unit,
) {
    LaunchedEffect(viewModel.newGroupState.isCompleted) {
        if (viewModel.newGroupState.isCompleted) {
            onNavigateBack()
        }
    }

    SearchUsersAndAppsScreen(
        searchTitle = stringResource(id = R.string.label_add_participants),
        onOpenUserProfile = onOpenUserProfile,
        onContactChecked = viewModel::updateSelectedContacts,
        onClose = onNavigateBack,
        onAppClicked = onOpenService,
        navigationIconType = NavigationIconType.Close(R.string.content_description_add_participants_close),
        itemActionType = ItemActionType.CHECK,
        selectedContacts = viewModel.newGroupState.selectedContacts,
        isAppsTabVisible = navArgs.isSelfPartOfATeam,
        isConversationAppsEnabled = navArgs.isConversationAppsEnabled,
        shouldHideBottomActionForServices = true,
        conversationProtocol = navArgs.protocolInfo,
        peopleBottomActions = { focusRequester ->
            ContinueButton(
                onContinue = viewModel::addMembersToConversation,
                buttonModifier = Modifier.focusRequester(focusRequester),
            )
        }
    )
}
