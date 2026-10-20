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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.di.KaliumAppApi
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.app.SyncAppsUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class AppsModule {

    @Provides
    fun provideAppApi(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): KaliumAppApi = coreLogic.getSessionScope(currentAccount).appApiFactory.create()

    @Provides
    fun provideGetAppByIdUseCase(api: KaliumAppApi): GetAppByIdUseCase =
        api.appGetAppById

    @Provides
    fun provideObserveIsAppMemberUseCase(api: KaliumAppApi): ObserveIsAppMemberUseCase =
        api.appObserveIsAppMember

    @Provides
    fun provideSearchAppsByNameUseCase(api: KaliumAppApi): SearchAppsByNameUseCase =
        api.appSearchAppsByName

    @Provides
    fun provideObserveAllAppsUseCase(api: KaliumAppApi): ObserveAllAppsUseCase =
        api.appObserveAllApps

    @Provides
    fun provideSyncAppsUseCase(api: KaliumAppApi): SyncAppsUseCase =
        api.appSyncApps
}
