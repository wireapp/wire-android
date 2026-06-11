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
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.ClientScope
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
    fun provideClientScopeProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ClientScope = coreLogic.getSessionScope(currentAccount).client

    @Provides
    fun provideMlsKeyPackageCountUseCase(clientScope: ClientScope): MLSKeyPackageCountUseCase =
        clientScope.mlsKeyPackageCountUseCase

    @Provides
    fun provideRestartSlowSyncProcessForRecoveryUseCase(clientScope: ClientScope): RestartSlowSyncProcessForRecoveryUseCase =
        clientScope.restartSlowSyncProcessForRecoveryUseCase

    @Provides
    fun provideDeleteClientUseCase(clientScope: ClientScope): DeleteClientUseCase = clientScope.deleteClient

    @Provides
    fun provideGetOrRegisterClientUseCase(clientScope: ClientScope): GetOrRegisterClientUseCase =
        clientScope.getOrRegister

    @Provides
    fun provideFetchUsersClientsFromRemoteUseCase(clientScope: ClientScope): FetchUsersClientsFromRemoteUseCase =
        clientScope.fetchUsersClients

    @Provides
    fun provideGetOtherUsersClients(clientScope: ClientScope): ObserveClientsByUserIdUseCase =
        clientScope.getOtherUserClients

    @Provides
    fun provideFetchSelfClientsFromRemoteUseCase(clientScope: ClientScope): FetchSelfClientsFromRemoteUseCase =
        clientScope.fetchSelfClients

    @Provides
    fun provideClientFingerPrintUseCase(clientScope: ClientScope): ClientFingerprintUseCase =
        clientScope.remoteClientFingerPrint

    @Provides
    fun provideUpdateClientVerificationStatusUseCase(clientScope: ClientScope): UpdateClientVerificationStatusUseCase =
        clientScope.updateClientVerificationStatus

    @Provides
    fun provideGetClientDetailsUseCase(clientScope: ClientScope): ObserveClientDetailsUseCase = clientScope.observeClientDetailsUseCase

    @Provides
    fun provideObserveCurrentClientUseCase(clientScope: ClientScope): ObserveCurrentClientIdUseCase =
        clientScope.observeCurrentClientId

    @Provides
    fun provideNeedsToRegisterClientUseCase(clientScope: ClientScope): NeedsToRegisterClientUseCase =
        clientScope.needsToRegisterClient
}
