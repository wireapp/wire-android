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
package com.wire.android.ui.home.newconversation.groupsearch

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.ItemActionType
import com.wire.android.search.SearchUsersAndAppsScreen
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.ContinueButton

/**
 * Navigation-neutral adapter. Cross-feature targets are exposed as callbacks until their typed
 * routes are owned by their respective migration batches.
 */
@Composable
internal fun NewGroupConversationSearchPeopleRouteScreen(
    newConversationViewModel: NewConversationViewModel,
    onNavigateBack: () -> Unit,
    onOpenUserProfile: (value: String, domain: String) -> Unit,
    onContinue: () -> Unit,
) {
    val onBackClicked = remember(Unit) {
        {
            newConversationViewModel.resetState()
            onNavigateBack()
        }
    }

    BackHandler(true, onBackClicked)

    val screenTitle = if (newConversationViewModel.newGroupState.isChannel) {
        stringResource(id = R.string.label_new_channel)
    } else {
        stringResource(id = R.string.label_new_group)
    }
    SearchUsersAndAppsScreen(
        searchTitle = screenTitle,
        onOpenUserProfile = { contact ->
            onOpenUserProfile(contact.id, contact.domain)
        },
        onContactChecked = newConversationViewModel::updateSelectedContacts,
        onClose = onBackClicked,
        navigationIconType = NavigationIconType.Back(R.string.content_description_new_conversation_back_btn),
        itemActionType = ItemActionType.CHECK,
        selectedContacts = newConversationViewModel.newGroupState.selectedUsers,
        onAppClicked = { },
        isAppsTabVisible = false,
        conversationProtocol = null,
        peopleBottomActions = { focusRequester ->
            ContinueButton(
                onContinue = onContinue,
                buttonModifier = Modifier.focusRequester(focusRequester),
            )
        }
    )
}
