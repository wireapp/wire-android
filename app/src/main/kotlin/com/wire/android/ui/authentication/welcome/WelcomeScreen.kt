/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.configuration.server.ServerConfig

internal sealed interface WelcomeScreenAction {
    data class Login(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class OpenUrl(val url: String) : WelcomeScreenAction
    data class CreateTeam(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreatePersonal(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreateAccountData(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
}

internal fun ServerConfig.Links.isProxyEnabled(): Boolean = apiProxy != null

/** Navigation-neutral adapter; the runtime translates semantic actions into concrete routes. */
@Composable
internal fun WelcomeRouteScreen(
    viewModel: WelcomeViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    WelcomeContent(
        isThereActiveSession = viewModel.state.isThereActiveSession,
        maxAccountsReached = viewModel.state.maxAccountsReached,
        nomadAccountBlocksLogin = viewModel.state.nomadAccountBlocksLogin,
        state = viewModel.state.links,
        navigateBack = onNavigateBack,
        onAction = onAction,
    )
}

@Preview
@Composable
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            isThereActiveSession = false,
            maxAccountsReached = false,
            nomadAccountBlocksLogin = false,
            state = ServerConfig.DEFAULT,
            navigateBack = {},
            onAction = {},
        )
    }
}
