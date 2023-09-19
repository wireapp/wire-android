/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.PersistOtherUserClientsUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ClientModule {

    @ViewModelScoped
    @Provides
    fun provideClientScopeProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ClientScope = coreLogic.getSessionScope(currentAccount).client

    @ViewModelScoped
    @Provides
    fun provideMlsKeyPackageCountUseCase(clientScope: ClientScope): MLSKeyPackageCountUseCase =
        clientScope.mlsKeyPackageCountUseCase

    @ViewModelScoped
    @Provides
    fun provideRestartSlowSyncProcessForRecoveryUseCase(clientScope: ClientScope): RestartSlowSyncProcessForRecoveryUseCase =
        clientScope.restartSlowSyncProcessForRecoveryUseCase

    @ViewModelScoped
    @Provides
    fun provideDeleteClientUseCase(clientScope: ClientScope): DeleteClientUseCase = clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun provideGetOrRegisterClientUseCase(clientScope: ClientScope): GetOrRegisterClientUseCase =
        clientScope.getOrRegister

    @ViewModelScoped
    @Provides
    fun providePersistOtherUsersClients(clientScope: ClientScope): PersistOtherUserClientsUseCase =
        clientScope.persistOtherUserClients

    @ViewModelScoped
    @Provides
    fun provideGetOtherUsersClients(clientScope: ClientScope): ObserveClientsByUserIdUseCase =
        clientScope.getOtherUserClients

    @ViewModelScoped
    @Provides
    fun provideSelfClientsUseCase(clientScope: ClientScope): FetchSelfClientsFromRemoteUseCase =
        clientScope.selfClients

    @ViewModelScoped
    @Provides
    fun provideClientFingerPrintUseCase(clientScope: ClientScope): ClientFingerprintUseCase =
        clientScope.remoteClientFingerPrint

    @ViewModelScoped
    @Provides
    fun provideUpdateClientVerificationStatusUseCase(clientScope: ClientScope): UpdateClientVerificationStatusUseCase =
        clientScope.updateClientVerificationStatus

    @ViewModelScoped
    @Provides
    fun provideGetClientDetailsUseCase(clientScope: ClientScope): ObserveClientDetailsUseCase = clientScope.observeClientDetailsUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveCurrentClientUseCase(clientScope: ClientScope): ObserveCurrentClientIdUseCase =
        clientScope.observeCurrentClientId

    @ViewModelScoped
    @Provides
    fun provideNeedsToRegisterClientUseCase(clientScope: ClientScope): NeedsToRegisterClientUseCase =
        clientScope.needsToRegisterClient
}
