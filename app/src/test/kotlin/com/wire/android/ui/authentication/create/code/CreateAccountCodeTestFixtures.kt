package com.wire.android.ui.authentication.create.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.register.RegisterAccountUseCase
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal fun personalRequest(code: () -> String = { "123456" }) =
    CreateAccountRegistrationRequest.Personal("Alice", "Wire", "secret", "alice@example.com", code)

internal fun credentials(): KaliumCreateAccountCredentials {
    val result = mockk<RegisterResult.Success>()
    every { result.authData } returns mockk<AccountTokens>()
    every { result.ssoID } returns null
    every { result.serverConfigId } returns "server"
    every { result.proxyCredentials } returns null
    return KaliumCreateAccountCredentials(result)
}

internal class CreateAccountCodeArrangement(
    defaultWebSocket: Boolean = false,
    buildFlags: CreateAccountCodeBuildFlags = CreateAccountCodeBuildFlags(false, "prod", "debug"),
) {
    val autoVersionAuthScope = mockk<AutoVersionAuthScopeUseCase>()
    val authenticationScope = mockk<AuthenticationScope>()
    val requestActivationCode = mockk<RequestActivationCodeUseCase>()
    val register = mockk<RegisterAccountUseCase>()
    val addAuthenticatedUser = mockk<AddAuthenticatedUserUseCase>()
    val clientScopeProviderFactory = mockk<ClientScopeProvider.Factory>()
    val clientScopeProvider = mockk<ClientScopeProvider>()
    val clientScope = mockk<ClientScope>()
    val getOrRegister = mockk<GetOrRegisterClientUseCase>()
    val gateway = KaliumCreateAccountCodeGateway(
        mockk<CoreLogic>().also { coreLogic ->
            coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScope
        },
        addAuthenticatedUser,
        clientScopeProviderFactory,
        defaultWebSocket,
        buildFlags,
    )

    init {
        every { authenticationScope.registerScope.requestActivationCode } returns requestActivationCode
        every { authenticationScope.registerScope.register } returns register
        every { clientScopeProviderFactory.create(any()) } returns clientScopeProvider
        every { clientScopeProvider.clientScope } returns clientScope
        every { clientScope.getOrRegister } returns getOrRegister
    }

    fun withAuthScope() = apply {
        coEvery { autoVersionAuthScope(null) } returns AutoVersionAuthScopeUseCase.Result.Success(authenticationScope)
    }
}

internal fun createAccountCodeArrangement(
    defaultWebSocket: Boolean = false,
    buildFlags: CreateAccountCodeBuildFlags = CreateAccountCodeBuildFlags(false, "prod", "debug"),
) = CreateAccountCodeArrangement(defaultWebSocket, buildFlags)
