/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import com.wire.kalium.logic.configuration.server.ServerConfig

internal sealed interface WelcomeScreenAction {
    data class Login(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class OpenUrl(val url: String) : WelcomeScreenAction
    data class CreateTeam(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreatePersonal(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreateAccountData(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
}

internal fun WelcomeAction<ServerConfig.Links>.toRouteAction(): WelcomeScreenAction = when (this) {
    is WelcomeAction.Login -> WelcomeScreenAction.Login(links)
    is WelcomeAction.OpenUrl -> WelcomeScreenAction.OpenUrl(url)
    is WelcomeAction.CreateTeam -> WelcomeScreenAction.CreateTeam(links)
    is WelcomeAction.CreatePersonal -> WelcomeScreenAction.CreatePersonal(links)
    is WelcomeAction.CreateAccountData -> WelcomeScreenAction.CreateAccountData(links)
}
