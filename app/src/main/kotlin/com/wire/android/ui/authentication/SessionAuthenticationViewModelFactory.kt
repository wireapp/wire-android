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
package com.wire.android.ui.authentication

import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.register.AndroidRegisterDeviceResendTimer
import com.wire.android.ui.authentication.devices.register.KaliumRegisterDeviceGateway
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dev.zacsweers.metro.Inject

/** Creates only ViewModels whose dependencies belong to one explicit session graph. */
@Suppress("LongParameterList")
class SessionAuthenticationViewModelFactory @Inject constructor(
    @CurrentAccount private val currentAccount: UserId,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val getOrRegisterClient: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val getSelfUser: GetSelfUserUseCase,
    private val requestSecondFactorVerificationCode: RequestSecondFactorVerificationCodeUseCase,
    private val countdownTimer: CountdownTimer,
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val deleteClient: DeleteClientUseCase,
    private val currentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val switchAccount: AccountSwitchUseCase,
    private val logout: LogoutUseCase,
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val setUserHandle: SetUserHandleUseCase,
    private val finalizeRegistrationAnalyticsMetadata: FinalizeRegistrationAnalyticsMetadataUseCase,
    private val registrationAnalyticsManager: RegistrationAnalyticsManagerUseCase,
) {
    fun registerDeviceViewModel() = RegisterDeviceViewModel(
        gateway = KaliumRegisterDeviceGateway(
            registerClient = getOrRegisterClient,
            isPasswordRequired = isPasswordRequired,
            userDataStore = userDataStoreProvider.getOrCreate(currentAccount),
            getSelfUser = getSelfUser,
            requestSecondFactorVerificationCode = requestSecondFactorVerificationCode,
        ),
        resendCodeTimer = AndroidRegisterDeviceResendTimer(countdownTimer),
    )

    fun removeDeviceViewModel() = RemoveDeviceViewModel(
        fetchSelfClientsFromRemote = fetchSelfClientsFromRemote,
        deleteClientUseCase = deleteClient,
        registerClientUseCase = getOrRegisterClient,
        isPasswordRequired = isPasswordRequired,
        userDataStore = userDataStoreProvider.getOrCreate(currentAccount),
        getSelfUser = getSelfUser,
        requestSecondFactorVerificationCodeUseCase = requestSecondFactorVerificationCode,
    )

    fun clearSessionViewModel(cancelUserId: UserId?) = ClearSessionViewModel(
        currentSession = currentSession,
        deleteSession = deleteSession,
        switchAccount = switchAccount,
        logout = logout,
        cancelUserId = cancelUserId,
    )

    fun createAccountUsernameViewModel() = CreateAccountUsernameViewModel(
        validateUserHandleUseCase = validateUserHandle,
        setUserHandleUseCase = setUserHandle,
        finalizeRegistrationAnalyticsMetadata = finalizeRegistrationAnalyticsMetadata,
        registrationAnalyticsManager = registrationAnalyticsManager,
    )
}
