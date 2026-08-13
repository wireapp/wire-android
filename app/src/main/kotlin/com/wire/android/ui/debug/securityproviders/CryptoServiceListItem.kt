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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItem
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CryptoServiceListItem(
    row: CryptoServiceRow,
    modifier: Modifier = Modifier,
) {
    RowItem(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensions().spacing16x),
        clickable = Clickable(enabled = false),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(row.labelRes),
                style = typography().body02,
                color = colorsScheme().onBackground,
            )
            Text(
                text = row.lookup,
                style = typography().label01,
                color = colorsScheme().secondaryText,
            )
            Text(
                text = stringResource(
                    R.string.debug_settings_crypto_service_resolved,
                    row.algorithm,
                    row.providerName,
                    row.providerVersion,
                ),
                style = typography().body02,
                color = colorsScheme().onBackground,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCryptoServiceListItem() = WireTheme {
    CryptoServiceListItem(
        row = CryptoServiceRow(
            labelRes = R.string.debug_settings_crypto_asset_key,
            lookup = "KeyGenerator.getInstance(\"AES\")",
            algorithm = "AES",
            providerName = "AndroidOpenSSL",
            providerVersion = "1.0",
        )
    )
}
