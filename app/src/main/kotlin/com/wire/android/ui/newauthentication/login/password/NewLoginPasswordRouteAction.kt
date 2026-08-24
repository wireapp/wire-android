/* Wire Copyright (C) 2026 Wire Swiss GmbH */
@file:Suppress("Filename", "MatchingDeclarationName")

package com.wire.android.ui.newauthentication.login.password

import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId

internal sealed interface NewLoginPasswordScreenAction {
    data class Success(val initialSyncCompleted: Boolean, val isE2EIRequired: Boolean, val userId: UserId) : NewLoginPasswordScreenAction
    data class RemoveDevice(val userId: UserId) : NewLoginPasswordScreenAction
    data object Canceled : NewLoginPasswordScreenAction
    data class VerificationRequired(val navArgs: LoginNavArgs) : NewLoginPasswordScreenAction
    data class CreateAccountSelector(val serverConfig: ServerConfig.Links, val email: String) : NewLoginPasswordScreenAction
    data class CreatePersonalAccount(val serverConfig: ServerConfig.Links) : NewLoginPasswordScreenAction
}
