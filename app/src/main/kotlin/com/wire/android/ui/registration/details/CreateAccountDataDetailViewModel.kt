/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.details

import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.model.AnalyticsEvent.RegistrationPersonalAccount
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.legacyregistration.details.LegacyActivationCodeResult
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsGateway
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsInput
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsViewModel
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/** Metro/Kalium adapter; validation and registration state machine live in :features:authentication. */
class CreateAccountDataDetailViewModel @AssistedInject constructor(
    @Assisted val createAccountNavArgs: CreateAccountDataNavArgs,
    validatePassword: ValidatePasswordUseCase,
    validateEmail: ValidateEmailUseCase,
    globalDataStore: GlobalDataStore,
    analytics: RegistrationAnalyticsManagerUseCase,
    @KaliumCoreLogic coreLogic: CoreLogic,
    defaultServerConfig: ServerConfig.Links,
) : LegacyRegistrationDetailsViewModel<ServerConfig.Links, CoreFailure>(
    input = LegacyRegistrationDetailsInput(createAccountNavArgs.customServerConfig, createAccountNavArgs.userRegistrationInfo.email),
    defaultServerConfig = defaultServerConfig,
    gateway = KaliumLegacyRegistrationDetailsGateway(validatePassword, validateEmail, globalDataStore, analytics, coreLogic),
) {
    @AssistedFactory interface Factory { fun create(createAccountNavArgs: CreateAccountDataNavArgs): CreateAccountDataDetailViewModel }
    val detailsState get() = state
    fun tosUrl(): String = serverConfig.tos
    fun teamCreationUrl(): String = serverConfig.teams
    fun onDetailsErrorDismiss() = onErrorDismiss()
}

private class KaliumLegacyRegistrationDetailsGateway(
    private val validatePassword: ValidatePasswordUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val globalDataStore: GlobalDataStore,
    private val analytics: RegistrationAnalyticsManagerUseCase,
    private val coreLogic: CoreLogic,
) : LegacyRegistrationDetailsGateway<ServerConfig.Links, CoreFailure> {
    override fun isPasswordValid(password: String) = validatePassword(password).isValid
    override fun isEmailValid(email: String) = validateEmail(email)
    override suspend fun setAnonymousRegistrationEnabled(enabled: Boolean) = globalDataStore.setAnonymousRegistrationEnabled(enabled)
    override fun onAccountSetup(withPasswordTries: Boolean) = analytics.sendEventIfEnabled(RegistrationPersonalAccount.AccountSetup(withPasswordTries))
    override fun onTermsOfUseDialog() = analytics.sendEventIfEnabled(RegistrationPersonalAccount.TermsOfUseDialog)
    override suspend fun requestActivationCode(serverConfig: ServerConfig.Links, email: String): LegacyActivationCodeResult<CoreFailure> {
        val scope = when (val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
            is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion, is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> return LegacyActivationCodeResult.AuthScopeUnavailable
        }
        return when (val result = scope.registerScope.requestActivationCode(email)) {
            RequestActivationCodeResult.Success -> LegacyActivationCodeResult.Sent
            RequestActivationCodeResult.Failure.AlreadyInUse -> LegacyActivationCodeResult.AlreadyInUse
            RequestActivationCodeResult.Failure.BlacklistedEmail -> LegacyActivationCodeResult.Blacklisted
            RequestActivationCodeResult.Failure.DomainBlocked -> LegacyActivationCodeResult.DomainBlocked
            RequestActivationCodeResult.Failure.InvalidEmail -> LegacyActivationCodeResult.InvalidEmail
            is RequestActivationCodeResult.Failure.Generic -> LegacyActivationCodeResult.Generic(result.failure)
        }
    }
}
