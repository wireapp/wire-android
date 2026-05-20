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
package com.wire.android.ui.home.conversations.search

import com.wire.android.mapper.ContactMapper
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import dev.zacsweers.metro.Inject

@Inject
class SearchUserViewModelFactory(
    private val searchUserUseCase: SearchUsersUseCase,
    private val searchByHandleUseCase: SearchByHandleUseCase,
    private val contactMapper: ContactMapper,
    private val federatedSearchParser: FederatedSearchParser,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val isFederationSearchAllowed: IsFederationSearchAllowedUseCase,
) {
    fun create(addMembersSearchNavArgs: AddMembersSearchNavArgs?): SearchUserViewModel = SearchUserViewModel(
        addMembersSearchNavArgs = addMembersSearchNavArgs,
        searchUserUseCase = searchUserUseCase,
        searchByHandleUseCase = searchByHandleUseCase,
        contactMapper = contactMapper,
        federatedSearchParser = federatedSearchParser,
        validateUserHandle = validateUserHandle,
        isFederationSearchAllowed = isFederationSearchAllowed,
    )
}
