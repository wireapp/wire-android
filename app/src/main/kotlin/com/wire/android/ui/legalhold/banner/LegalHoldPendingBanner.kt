/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.legalhold.banner

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LegalHoldPendingBanner(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LegalHoldBaseBanner(onClick = onClick, modifier = modifier) {
        Row {
            Text(
                text = stringResource(id = R.string.legal_hold_is_pending_label),
                style = typography().label01,
                color = colorsScheme().onSurface,
            )
            Text(
                text = stringResource(id = R.string.legal_hold_accept),
                style = typography().label02,
                textDecoration = TextDecoration.Underline,
                color = colorsScheme().onSurface,
                modifier = Modifier.padding(start = dimensions().spacing2x),
            )
        }
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldPendingBanner() {
    WireTheme {
        LegalHoldPendingBanner()
    }
}
