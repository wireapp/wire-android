/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun NewLoginHeader(
    serverConfig: ServerConfig.Links,
    presentation: NewLoginContentPresentation,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
) {
    val credentials = presentation.mode == NewLoginContentMode.Identifier
    NewAuthHeader(
        title = {
            if (serverConfig.isOnPremises) OnPremisesTitle(serverConfig, credentials)
            else CloudTitle(presentation)
        },
        canNavigateBack = canNavigateBack,
        onNavigateBack = navigateBack,
    )
}

@Composable
private fun OnPremisesTitle(config: ServerConfig.Links, credentials: Boolean) {
    ServerTitle(
        serverLinks = config,
        style = typography().title01,
        textColor = colorsScheme().onSurface,
        titleResId = R.string.enterprise_login_on_prem_welcome_title,
        modifier = if (credentials) Modifier.padding(bottom = dimensions().spacing24x) else Modifier,
    )
    if (credentials) NewAuthSubtitle(stringResource(AuthenticationR.string.enterprise_login_credentials_title))
}

@Composable
private fun CloudTitle(presentation: NewLoginContentPresentation) {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_wire_logo),
        tint = MaterialTheme.colorScheme.onBackground,
        contentDescription = null,
        modifier = Modifier.padding(horizontal = dimensions().spacing32x).size(dimensions().spacing120x),
    )
    NewAuthSubtitle(
        title = when (presentation.mode) {
            NewLoginContentMode.BackendConfiguration -> stringResource(R.string.missing_backend_config_title)
            NewLoginContentMode.BackendConfigurationSuccess -> ""
            NewLoginContentMode.Identifier -> stringResource(AuthenticationR.string.enterprise_login_welcome)
        },
        modifier = Modifier.padding(top = dimensions().spacing16x),
    )
}
