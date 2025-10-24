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

package com.wire.android.ui.theme

// import com.wire.android.navigation.rememberNavigator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState

@Composable
fun WireTheme(
    wireColorScheme: WireColorScheme = WireColorSchemeTypes.currentTheme,
    wireFixedColorScheme: WireFixedColorScheme = DefaultWireFixedColorScheme,
    wireTypography: WireTypography = WireTypographyTypes.currentScreenSize,
    wireDimensions: WireDimensions = WireDimensionsTypes.currentScreenSize.currentOrientation,
    content: @Composable () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    @Suppress("SpreadOperator")
    CompositionLocalProvider(
        LocalWireColors provides wireColorScheme,
        LocalWireFixedColors provides wireFixedColorScheme,
        LocalWireTypography provides wireTypography,
        LocalWireDimensions provides wireDimensions,
        // we need to provide our default content color dependent on the current colorScheme, otherwise it's Color.Black
        LocalContentColor provides wireColorScheme.onBackground,
        *if (isPreview) {
            arrayOf(LocalSnackbarHostState provides remember { SnackbarHostState() })
        } else {
            emptyArray()
        },
    ) {
        MaterialTheme(
            colorScheme = wireColorScheme.toColorScheme(),
            typography = wireTypography.toTypography()
        ) {
            if (!isPreview) {
                UpdateSystemBarIconsAppearance(wireColorScheme.useDarkSystemBarIcons)
            }
            content()
        }
    }
}

@Composable
fun WireColorScheme(
    wireColorScheme: WireColorScheme = WireColorSchemeTypes.currentTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalWireColors provides wireColorScheme
    ) {
        MaterialTheme(
            colorScheme = wireColorScheme.toColorScheme()
        ) {
            content()
        }
    }
}

private val LocalWireColors = staticCompositionLocalOf { WireColorSchemeTypes.light }
private val LocalWireFixedColors = staticCompositionLocalOf { DefaultWireFixedColorScheme }
private val LocalWireTypography = staticCompositionLocalOf { WireTypographyTypes.defaultPhone }
private val LocalWireDimensions = staticCompositionLocalOf { WireDimensionsTypes.defaultPhone.portrait }

val MaterialTheme.wireColorScheme
    @Composable
    get() = LocalWireColors.current

val MaterialTheme.wireDarkColorScheme
    @Composable
    get() = WireColorSchemeTypes.dark

val MaterialTheme.wireLightColorScheme
    @Composable
    get() = WireColorSchemeTypes.light

val MaterialTheme.wireFixedColorScheme
    @Composable
    get() = LocalWireFixedColors.current

val MaterialTheme.wireTypography
    @Composable
    get() = LocalWireTypography.current

val MaterialTheme.wireDimensions
    @Composable
    get() = LocalWireDimensions.current
