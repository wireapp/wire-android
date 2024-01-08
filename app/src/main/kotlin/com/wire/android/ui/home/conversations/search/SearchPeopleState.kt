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

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.newconversation.model.Contact
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class SearchPeopleState(
    val initialContacts: SearchResultState = SearchResultState.Initial,
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchResult: ImmutableMap<SearchResultTitle, ContactSearchResult> = persistentMapOf(),
    val noneSearchSucceed: Boolean = false,
    val contactsAddedToGroup: ImmutableList<Contact> = persistentListOf(),
    val isGroupCreationContext: Boolean = true,
    val servicesInitialContacts: SearchResultState = SearchResultState.Initial,
    val servicesSearchResult: SearchResultState = SearchResultState.Initial,
    val isServicesAllowed: Boolean = false
)

sealed class ContactSearchResult(val searchResultState: SearchResultState) {
    class InternalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState
        )

    class ExternalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState
        )
}

sealed class SearchResultState {
    object Initial : SearchResultState()
    object InProgress : SearchResultState()
    data class Failure(@StringRes val failureString: Int) : SearchResultState()

    object EmptyResult : SearchResultState()

    data class Success(val result: ImmutableList<Contact>) : SearchResultState()
}

data class SearchResultTitle(@StringRes val stringRes: Int)
