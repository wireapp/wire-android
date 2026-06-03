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

package com.wire.android.di

import com.wire.android.datastore.UserDataStore
import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dagger.Module
import dagger.Provides

@Module
@Suppress("TooManyFunctions")
class NoSessionAuthenticationModule {

    @Provides
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic): CurrentSessionFlowUseCase =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @Provides
    fun provideDoesValidSessionExistsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidSessionExistUseCase =
        coreLogic.getGlobalScope().doesValidSessionExist

    @Provides
    fun provideGetServerConfigUserCase(@KaliumCoreLogic coreLogic: CoreLogic): GetServerConfigUseCase =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @Provides
    fun provideObserveIfAppFreshEnoughUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveIfAppUpdateRequiredUseCase =
        coreLogic.getGlobalScope().observeIfAppUpdateRequired

    @Provides
    fun provideObserveNewClientsUseCaseUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveNewClientsUseCase =
        coreLogic.getGlobalScope().observeNewClientsUseCase

    @Provides
    fun provideClearNewClientsForUser(@KaliumCoreLogic coreLogic: CoreLogic): ClearNewClientsForUserUseCase =
        coreLogic.getGlobalScope().clearNewClientsForUser

    @Provides
    fun provideDoesValidNomadAccountExistUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidNomadAccountExistUseCase =
        coreLogic.getGlobalScope().doesValidNomadAccountExist

    @Provides
    fun provideAddAuthenticatedUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): AddAuthenticatedUserUseCase =
        coreLogic.getGlobalScope().addAuthenticatedAccount

    @Provides
    fun provideValidateEmailUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateEmailUseCase =
        coreLogic.getGlobalScope().validateEmailUseCase

    @Provides
    fun provideValidateSSOCodeUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateSSOCodeUseCase =
        coreLogic.getGlobalScope().validateSSOCodeUseCase

    @Provides
    fun provideValidatePasswordUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidatePasswordUseCase =
        coreLogic.getGlobalScope().validatePasswordUseCase

    @Provides
    fun provideValidateUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateUserHandleUseCase =
        coreLogic.getGlobalScope().validateUserHandleUseCase

    @Provides
    fun provideUnavailableUserDataStore(): UserDataStore = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableLogoutUseCase(): LogoutUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableGetOrRegisterClientUseCase(): GetOrRegisterClientUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableIsPasswordRequiredUseCase(): IsPasswordRequiredUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableGetSelfUserUseCase(): GetSelfUserUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableRequestSecondFactorVerificationCodeUseCase(): RequestSecondFactorVerificationCodeUseCase =
        unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableFetchSelfClientsFromRemoteUseCase(): FetchSelfClientsFromRemoteUseCase =
        unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableDeleteClientUseCase(): DeleteClientUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableSetUserHandleUseCase(): SetUserHandleUseCase = unavailableSessionAuthBinding()

    @Provides
    fun provideUnavailableFinalizeRegistrationAnalyticsMetadataUseCase(): FinalizeRegistrationAnalyticsMetadataUseCase =
        unavailableSessionAuthBinding()

    private inline fun <reified T> unavailableSessionAuthBinding(): T =
        error("${T::class.qualifiedName} requires WireSessionGraph(userId), not WireAppGraph")
}
