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
package com.wire.android.util.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme

/**
 * A lightweight wrapper theme used **only in Compose Previews** to simplify layout visualization.
 *
 * It wraps content in [WireTheme], adds a scrollable [Column], and sets a background color
 * matching the current [colorsScheme].
 * Useful when your preview contains several large composables (e.g. tall messages or image galleries)
 * that may exceed preview bounds.
 *
 * ⚠️ **Important:**
 * Do **not** use this composable in production UI code — it's intended purely for preview/testing purposes.
 *
 * Example usage:
 * ```
 * @PreviewMultipleScreens
 * @Composable
 * fun PreviewTallMessages() {
 *     WireScrollableThemePreview {
 *         RegularMessageItem(...)
 *         RegularMessageItem(...)
 *     }
 * }
 * ```
 */
@SuppressLint("ComposeModifierMissing")
@Composable
fun WireScrollableThemePreview(
    content: @Composable ColumnScope.() -> Unit = {},
) {
    WireTheme {
        val scroll = rememberScrollState()
        Box(
            Modifier
                .fillMaxSize()
                .background(colorsScheme().surface)
        ) {
            Column(Modifier.verticalScroll(scroll)) {
                content()
            }
        }
    }
}
