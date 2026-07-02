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

package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity

@Composable
@ReadOnlyComposable
fun maxTitleLines(): Int = if (LocalDensity.current.fontScale > DEFAULT_FONT_SCALE) {
    EXPANDED_FONT_SCALE_MAX_LINES
} else {
    DEFAULT_FONT_SCALE_MAX_LINES
}

private const val DEFAULT_FONT_SCALE = 1f
private const val DEFAULT_FONT_SCALE_MAX_LINES = 1
private const val EXPANDED_FONT_SCALE_MAX_LINES = 4
