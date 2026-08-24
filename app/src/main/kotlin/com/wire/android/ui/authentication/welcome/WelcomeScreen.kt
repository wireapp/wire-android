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

/**
 * Navigation-neutral screen adapter. It keeps rendering and dialog behavior in the screen while
 * the owning runtime translates semantic actions into concrete routes.
 */
@Composable
internal fun WelcomeRouteScreen(
    viewModel: WelcomeViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    WelcomeContent(
        viewModel.state.isThereActiveSession,
        viewModel.state.maxAccountsReached,
        viewModel.state.nomadAccountBlocksLogin,
        viewModel.state.links,
        onNavigateBack,
        onAction = onAction,
    )
}

internal fun ServerConfig.Links.isProxyEnabled() = apiProxy != null
@Preview
@Composable
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            activeSession = false,
            maxAccounts = false,
            nomadBlocksLogin = false,
            links = ServerConfig.DEFAULT,
            navigateBack = {},
            onAction = {},
        )
    }
}
