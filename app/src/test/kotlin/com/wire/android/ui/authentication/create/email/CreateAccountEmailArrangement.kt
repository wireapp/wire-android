package com.wire.android.ui.authentication.create.email

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

internal class CreateAccountEmailArrangement(defaultServerConfig: ServerConfig.Links = ServerConfig.STAGING) {
    val validateEmail = mockk<ValidateEmailUseCase>()
    val coreLogic = mockk<CoreLogic>()
    val autoVersionAuthScope = mockk<AutoVersionAuthScopeUseCase>()
    val authenticationScope = mockk<AuthenticationScope>()
    val requestActivationCode = mockk<RequestActivationCodeUseCase>()
    val gateway = KaliumCreateAccountEmailGateway(validateEmail, coreLogic)
    val hostFactory = CreateAccountEmailViewModelHostFactory(validateEmail, coreLogic, defaultServerConfig)

    init {
        coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScope
        every { authenticationScope.registerScope.requestActivationCode } returns requestActivationCode
    }
}
