/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.create.code

import com.wire.android.BuildConfig
import com.wire.android.util.WillNeverOccurError
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult

internal fun RequestActivationCodeResult.toActivationCodeResult(): ActivationCodeRequestResult<CoreFailure> = when (this) {
    RequestActivationCodeResult.Success -> ActivationCodeRequestResult.Sent
    RequestActivationCodeResult.Failure.AlreadyInUse -> ActivationCodeRequestResult.AlreadyInUse
    RequestActivationCodeResult.Failure.BlacklistedEmail -> ActivationCodeRequestResult.Blacklisted
    RequestActivationCodeResult.Failure.DomainBlocked -> ActivationCodeRequestResult.DomainBlocked
    RequestActivationCodeResult.Failure.InvalidEmail -> ActivationCodeRequestResult.InvalidEmail
    is RequestActivationCodeResult.Failure.Generic -> ActivationCodeRequestResult.Generic(failure)
}

internal fun CreateAccountRegistrationRequest.toRegisterParam(): RegisterParam = when (this) {
    is CreateAccountRegistrationRequest.Personal -> RegisterParam.PrivateAccount(
        firstName,
        lastName,
        password,
        email,
        activationCode(),
    )
    is CreateAccountRegistrationRequest.Team -> RegisterParam.Team(
        firstName,
        lastName,
        password,
        email,
        activationCode(),
        teamName,
        teamIcon,
    )
}

internal fun RegisterResult.toRegistrationResult(): AccountRegistrationResult<CoreFailure, KaliumCreateAccountCredentials> = when (this) {
    is RegisterResult.Success -> AccountRegistrationResult.Success(KaliumCreateAccountCredentials(this))
    RegisterResult.Failure.InvalidActivationCode -> AccountRegistrationResult.InvalidActivationCode
    RegisterResult.Failure.AccountAlreadyExists -> AccountRegistrationResult.AccountAlreadyExists
    RegisterResult.Failure.BlackListed -> AccountRegistrationResult.Blacklisted
    RegisterResult.Failure.EmailDomainBlocked -> AccountRegistrationResult.DomainBlocked
    RegisterResult.Failure.InvalidEmail -> AccountRegistrationResult.InvalidEmail
    RegisterResult.Failure.TeamMembersLimitReached -> AccountRegistrationResult.TeamMembersLimitReached
    RegisterResult.Failure.UserCreationRestricted -> AccountRegistrationResult.UserCreationRestricted
    is RegisterResult.Failure.Generic -> AccountRegistrationResult.Generic(failure)
}

internal fun RegisterResult.Success.toStoreSessionParam(webSocketEnabled: Boolean) = StoreSessionParam(
    accountTokens = authData,
    ssoId = ssoID,
    serverConfigId = serverConfigId,
    proxyCredentials = proxyCredentials,
    isPersistentWebSocketEnabled = webSocketEnabled,
)

internal fun AddAuthenticatedUserUseCase.Result.toStoreSessionResult(): StoreAccountSessionResult<CoreFailure, UserId> = when (this) {
    is AddAuthenticatedUserUseCase.Result.Success -> StoreAccountSessionResult.Success(userId)
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> StoreAccountSessionResult.Generic(genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
    AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
    AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> StoreAccountSessionResult.UserAlreadyExists
}

internal fun RegisterClientResult.toCreateAccountClientResult(): CreateAccountClientResult<CoreFailure> = when (this) {
    is RegisterClientResult.Success -> CreateAccountClientResult.Success
    is RegisterClientResult.E2EICertificateRequired -> CreateAccountClientResult.E2EICertificateRequired
    RegisterClientResult.Failure.TooManyClients -> CreateAccountClientResult.TooManyDevices
    is RegisterClientResult.Failure.Generic -> CreateAccountClientResult.Generic(genericFailure)
    is RegisterClientResult.Failure.InvalidCredentials -> throw WillNeverOccurError(
        "RegisterClient: wrong password when register client after creating a new account",
    )
    RegisterClientResult.Failure.PasswordAuthRequired -> throw WillNeverOccurError(
        "RegisterClient: password required to register client after creating new account with email",
    )
}

internal data class CreateAccountCodeBuildFlags(val privateBuild: Boolean, val flavor: String, val buildType: String) {
    val modelPostfix: String? get() = if (privateBuild) " [${flavor}_$buildType]" else null
    companion object {
        fun current() = CreateAccountCodeBuildFlags(BuildConfig.PRIVATE_BUILD, BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE)
    }
}

internal class AndroidCreateAccountCodeResendTimer(private val countdownTimer: CountdownTimer) : CreateAccountCodeResendTimer {
    override suspend fun start(
        seconds: Long,
        onUpdate: (String) -> Unit,
        onFinish: () -> Unit
    ) = countdownTimer.start(seconds, onUpdate, onFinish)
}
