/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.cells.ui.publiclink.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.publiclink.PublicLinkPassword
import com.wire.android.feature.cells.ui.publiclink.PublicLinkSettings
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun PublicLinkSettingsSection(
    settings: PublicLinkSettings,
    onPasswordClick: () -> Unit,
    onExpirationClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.public_link_settings).uppercase(),
            style = typography().title03,
            modifier = Modifier.padding(dimensions().spacing16x)
        )

        LinkSettingsOption(
            isEnabled = settings.passwordSettings != null,
            title = stringResource(R.string.public_link_setting_password_title),
            subtitle = stringResource(R.string.public_link_setting_password_subtitle),
            onClick = onPasswordClick,
        )

        WireDivider()

        LinkSettingsOption(
            isEnabled = settings.expirationSettings != null,
            title = stringResource(R.string.public_link_setting_expiration_title),
            subtitle = stringResource(R.string.public_link_setting_expiration_subtitle),
            onClick = onExpirationClick,
        )
    }
}

@Composable
internal fun LinkSettingsOption(
    isEnabled: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().surface)
            .clickable { onClick() }
            .padding(dimensions().spacing12x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = title,
                style = typography().body02,
            )

            Text(
                text = stringResource(if (isEnabled) R.string.label_on else R.string.label_off),
                style = typography().body01,
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
            )
        }

        Text(
            text = subtitle,
            style = typography().body01,
            color = colorsScheme().secondaryText,
        )
    }
}

@Composable
@PreviewMultipleThemes
private fun PreviewSettingsOption() {
    WireTheme {
        PublicLinkSettingsSection(
            PublicLinkSettings(
                passwordSettings = PublicLinkPassword("preview"),
                expirationSettings = null,
            ),
            onPasswordClick = {},
            onExpirationClick = {},
        )
    }
}
