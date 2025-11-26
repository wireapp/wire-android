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

package com.wire.android.ui.home.conversations.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.newconversation.model.Contact
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberSearchPeopleScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): SearchPeopleScreenState {
    return remember {
        SearchPeopleScreenState(
            coroutineScope = coroutineScope,
        )
    }
}

class SearchPeopleScreenState(
    val coroutineScope: CoroutineScope,
) {

    private val newGroupContacts = mutableStateListOf<Contact>()

    var contactsAllResultsCollapsed: Boolean by mutableStateOf(false)

    var publicResultsCollapsed: Boolean by mutableStateOf(false)

    var federatedBackendResultsCollapsed: Boolean by mutableStateOf(false)

    fun toggleShowAllContactsResult() {
        contactsAllResultsCollapsed = !contactsAllResultsCollapsed
    }

    fun toggleShowAllPublicResult() {
        publicResultsCollapsed = !publicResultsCollapsed
    }

    fun toggleShowFederatedBackendResult() {
        federatedBackendResultsCollapsed = !federatedBackendResultsCollapsed
    }

    fun addContactToGroup(contact: Contact) {
        newGroupContacts.add(contact)
    }

    fun removeContactFromGroup(contact: Contact) {
        newGroupContacts.remove(contact)
    }
}
