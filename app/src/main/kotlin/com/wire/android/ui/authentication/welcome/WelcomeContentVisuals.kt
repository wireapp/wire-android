/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.isConfigured
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun WelcomeLogo() {
    Icon(
        ImageVector.vectorResource(R.drawable.ic_wire_logo),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
internal fun WelcomeServerTitle(links: ServerConfig.Links) {
    if (links.isOnPremises) {
        ServerTitle(
            links,
            Modifier.padding(
                top = dimensions().spacing16x,
                start = dimensions().spacing32x,
                end = dimensions().spacing32x,
            ),
        )
    }
}

@Composable
internal fun ServerConfig.Links.welcomeBodyOverride(): (@Composable ColumnScope.() -> Unit)? =
    if (!isConfigured()) {
        {
            MissingBackendConfigContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding)
                    .weight(1f, true),
                showTitle = true,
                centerText = true,
                verticalArrangement = Arrangement.Center,
            )
        }
    } else {
        null
    }
