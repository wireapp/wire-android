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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MultiFactorAuthenticationScreen(onBackPressed: () -> Unit) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.settings_multi_factor_authentication_label),
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
            )
        }
    ) { internalPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationScreen() = WireTheme {
    MultiFactorAuthenticationScreen(onBackPressed = {})
}
