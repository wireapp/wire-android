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
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.di.KaliumServiceApi
import com.wire.kalium.logic.feature.service.SyncServicesUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class ServicesModule {

    @Provides
    fun provideServiceApi(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): KaliumServiceApi = coreLogic.getSessionScope(currentAccount).serviceApiFactory.create()

    @Provides
    fun provideObserveIsServiceMemberUseCase(api: KaliumServiceApi): ObserveIsServiceMemberUseCase =
        api.serviceObserveIsServiceMember

    @Provides
    fun provideGetServiceByIdUseCase(api: KaliumServiceApi): GetServiceByIdUseCase =
        api.serviceGetServiceById

    @Provides
    fun provideObserveAllServicesUseCase(api: KaliumServiceApi): ObserveAllServicesUseCase =
        api.serviceObserveAllServices

    @Provides
    fun provideSyncServicesUseCase(api: KaliumServiceApi): SyncServicesUseCase =
        api.serviceSyncServices

    @Provides
    fun provideSearchServicesByNameUseCase(api: KaliumServiceApi): SearchServicesByNameUseCase =
        api.serviceSearchServicesByName

    @Provides
    fun provideObserveIsAppsAllowedForUsage(api: KaliumServiceApi): ObserveIsAppsAllowedForUsageUseCase =
        api.serviceObserveIsAppsAllowedForUsage
}
