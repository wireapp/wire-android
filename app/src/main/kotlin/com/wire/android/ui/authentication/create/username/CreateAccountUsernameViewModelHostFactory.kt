/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.username

import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dev.zacsweers.metro.Inject

internal class KaliumCreateAccountUsernameGateway(
    private val validateUserHandle: ValidateUserHandleUseCase,
    private val setUserHandle: SetUserHandleUseCase,
) : CreateAccountUsernameGateway<CoreFailure> {
    override fun validateUsername(username: String): UsernameValidation = when (validateUserHandle(username)) {
        is ValidateUserHandleResult.Valid -> UsernameValidation.Valid
        is ValidateUserHandleResult.Invalid -> UsernameValidation.Invalid
    }

    override suspend fun setUsername(username: String): SetUsernameResult<CoreFailure> =
        when (val result = setUserHandle(username)) {
            SetUserHandleResult.Success -> SetUsernameResult.Success
            SetUserHandleResult.Failure.HandleExists -> SetUsernameResult.UsernameTaken
            SetUserHandleResult.Failure.InvalidHandle -> SetUsernameResult.UsernameInvalid
            is SetUserHandleResult.Failure.Generic -> SetUsernameResult.Failure(result.error)
        }
}

internal class AppCreateAccountUsernameAnalytics(
    private val registrationAnalyticsManager: RegistrationAnalyticsManagerUseCase,
    private val finalizeRegistrationAnalyticsMetadata: FinalizeRegistrationAnalyticsMetadataUseCase,
) : CreateAccountUsernameAnalytics {
    override suspend fun usernameScreenShown() {
        registrationAnalyticsManager.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.Username)
    }

    override suspend fun accountCreationCompleted() {
        registrationAnalyticsManager.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CreationCompleted)
        finalizeRegistrationAnalyticsMetadata()
    }
}

class CreateAccountUsernameViewModelHostFactory @Inject constructor(
    validateUserHandle: ValidateUserHandleUseCase,
    setUserHandle: SetUserHandleUseCase,
    registrationAnalyticsManager: RegistrationAnalyticsManagerUseCase,
    finalizeRegistrationAnalyticsMetadata: FinalizeRegistrationAnalyticsMetadataUseCase,
) {
    private val gateway = KaliumCreateAccountUsernameGateway(validateUserHandle, setUserHandle)
    private val analytics = AppCreateAccountUsernameAnalytics(
        registrationAnalyticsManager,
        finalizeRegistrationAnalyticsMetadata,
    )

    fun create(): CreateAccountUsernameViewModel<CoreFailure> = CreateAccountUsernameViewModel(
        gateway = gateway,
        analytics = analytics,
    )
}
