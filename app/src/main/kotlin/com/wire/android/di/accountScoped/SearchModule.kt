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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchScope
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class SearchModule {

    @ViewModelScoped
    @Provides
    fun provideSearchScope(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): SearchScope = coreLogic.getSessionScope(currentAccount).search

    @ViewModelScoped
    @Provides
    fun provideSearchUsersUseCase(searchScope: SearchScope): SearchUsersUseCase = searchScope.searchUsers

    @ViewModelScoped
    @Provides
    fun provideSearchByHandleUseCase(searchScope: SearchScope): SearchByHandleUseCase = searchScope.searchByHandle

    @ViewModelScoped
    @Provides
    fun provideFederatedSearchParser(searchScope: SearchScope): FederatedSearchParser = searchScope.federatedSearchParser

    @ViewModelScoped
    @Provides
    fun provideIsFederationSearchAllowedUseCase(searchScope: SearchScope): IsFederationSearchAllowedUseCase =
        searchScope.isFederationSearchAllowedUseCase
}
