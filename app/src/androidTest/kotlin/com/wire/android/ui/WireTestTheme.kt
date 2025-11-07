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
package com.wire.android.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.DefaultWireFixedColorScheme
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireColorSchemeTypes
import com.wire.android.ui.theme.WireDimensions
import com.wire.android.ui.theme.WireDimensionsTypes
import com.wire.android.ui.theme.WireFixedColorScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.WireTypography
import com.wire.android.ui.theme.WireTypographyTypes

@Composable
fun WireTestTheme(
    wireColorScheme: WireColorScheme = WireColorSchemeTypes.currentTheme,
    wireFixedColorScheme: WireFixedColorScheme = DefaultWireFixedColorScheme,
    wireTypography: WireTypography = WireTypographyTypes.currentScreenSize,
    wireDimensions: WireDimensions = WireDimensionsTypes.currentScreenSize.currentOrientation,
    content: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            WireTheme(
                wireColorScheme,
                wireFixedColorScheme,
                wireTypography,
                wireDimensions,
                Accent.Unknown,
                content
            )
        }
}
