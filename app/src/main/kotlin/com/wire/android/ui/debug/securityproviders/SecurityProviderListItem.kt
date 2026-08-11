/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.securityproviders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItem
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun SecurityProviderListItem(
    provider: SecurityProvider,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        RowItem(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = dimensions().spacing48x)
                .padding(dimensions().spacing16x),
            clickable = Clickable { expanded = !expanded }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = listOf(provider.name, provider.version).filter(String::isNotBlank).joinToString(" "),
                    style = typography().body02,
                    color = colorsScheme().onBackground,
                )
                Text(
                    text = provider.info,
                    style = typography().label01,
                    color = colorsScheme().secondaryText,
                )
                Text(
                    text = stringResource(R.string.debug_settings_security_providers_entry_count, provider.entries.size),
                    style = typography().label01,
                    color = colorsScheme().secondaryText,
                )
            }

            Icon(
                painter = painterResource(
                    if (expanded) commonR.drawable.ic_collapse else commonR.drawable.ic_expand_more
                ),
                contentDescription = null,
                tint = colorsScheme().onSurfaceVariant,
            )
        }

        AnimatedVisibility(expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing16x,
                    )
            ) {
                provider.entries.forEach { entry ->
                    Text(
                        text = "${entry.key}: ${entry.value}",
                        modifier = Modifier.fillMaxWidth(),
                        style = typography().label01,
                        color = colorsScheme().secondaryText,
                    )
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSecurityProviderListItem() = WireTheme {
    SecurityProviderListItem(
        provider = SecurityProvider(
            name = "AndroidKeyStore",
            version = "1.0",
            info = "Android KeyStore security provider",
            entries = listOf(
                KeyValueEntry("KeyStore.AndroidKeyStore", "android.security.keystore2.AndroidKeyStoreProvider"),
                KeyValueEntry("Signature.SHA256withECDSA", "android.security.keystore2.AndroidKeyStoreSignatureSpi\$ECDSA"),
            )
        )
    )
}
