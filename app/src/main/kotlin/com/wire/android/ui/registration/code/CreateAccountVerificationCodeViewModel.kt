/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeInput
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeViewModel
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/** App-only Metro composition for the feature-owned legacy registration state machine. */
class CreateAccountVerificationCodeViewModel @AssistedInject constructor(
    @Assisted val createAccountNavArgs: CreateAccountDataNavArgs,
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    analytics: RegistrationAnalyticsManagerUseCase,
    clientScopes: ClientScopeProvider.Factory,
    defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault webSocketEnabled: Boolean,
) : LegacyRegistrationCodeViewModel<ServerConfig.Links, CoreFailure, UserId, KaliumLegacyRegistrationCredentials>(
    input = LegacyRegistrationCodeInput(
        customServerConfig = createAccountNavArgs.customServerConfig,
        email = createAccountNavArgs.userRegistrationInfo.email,
        name = createAccountNavArgs.userRegistrationInfo.name,
        password = createAccountNavArgs.userRegistrationInfo.password,
    ),
    defaultServerConfig = defaultServerConfig,
    gateway = KaliumLegacyRegistrationCodeGateway(
        coreLogic = coreLogic,
        addUser = addAuthenticatedUser,
        analytics = analytics,
        clientScopes = clientScopes,
        webSocketEnabled = webSocketEnabled,
    ),
) {
    @AssistedFactory
    interface Factory {
        fun create(createAccountNavArgs: CreateAccountDataNavArgs): CreateAccountVerificationCodeViewModel
    }
}
