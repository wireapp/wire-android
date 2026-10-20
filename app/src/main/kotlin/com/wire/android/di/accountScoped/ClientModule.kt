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
import com.wire.kalium.logic.di.KaliumClientApi
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class ClientModule {

    @Provides
    fun provideClientApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): KaliumClientApi = coreLogic.getSessionScope(currentAccount).clientApi

    @Provides
    fun provideMlsKeyPackageCountUseCase(api: KaliumClientApi): MLSKeyPackageCountUseCase =
        api.clientMlsKeyPackageCountUseCase

    @Provides
    fun provideRestartSlowSyncProcessForRecoveryUseCase(api: KaliumClientApi): RestartSlowSyncProcessForRecoveryUseCase =
        api.clientRestartSlowSyncProcessForRecoveryUseCase

    @Provides
    fun provideDeleteClientUseCase(api: KaliumClientApi): DeleteClientUseCase = api.clientDeleteClient

    @Provides
    fun provideGetOrRegisterClientUseCase(api: KaliumClientApi): GetOrRegisterClientUseCase =
        api.clientGetOrRegister

    @Provides
    fun provideFetchUsersClientsFromRemoteUseCase(api: KaliumClientApi): FetchUsersClientsFromRemoteUseCase =
        api.clientFetchUsersClients

    @Provides
    fun provideGetOtherUsersClients(api: KaliumClientApi): ObserveClientsByUserIdUseCase =
        api.clientGetOtherUserClients

    @Provides
    fun provideFetchSelfClientsFromRemoteUseCase(api: KaliumClientApi): FetchSelfClientsFromRemoteUseCase =
        api.clientFetchSelfClients

    @Provides
    fun provideClientFingerPrintUseCase(api: KaliumClientApi): ClientFingerprintUseCase =
        api.clientRemoteClientFingerPrint

    @Provides
    fun provideUpdateClientVerificationStatusUseCase(api: KaliumClientApi): UpdateClientVerificationStatusUseCase =
        api.clientUpdateClientVerificationStatus

    @Provides
    fun provideGetClientDetailsUseCase(api: KaliumClientApi): ObserveClientDetailsUseCase = api.clientObserveClientDetailsUseCase

    @Provides
    fun provideObserveCurrentClientUseCase(api: KaliumClientApi): ObserveCurrentClientIdUseCase =
        api.clientObserveCurrentClientId

    @Provides
    fun provideNeedsToRegisterClientUseCase(api: KaliumClientApi): NeedsToRegisterClientUseCase =
        api.clientNeedsToRegisterClient
}
