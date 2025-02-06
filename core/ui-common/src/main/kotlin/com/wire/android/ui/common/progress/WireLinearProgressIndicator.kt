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

package com.wire.android.ui.common.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun WireLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = colorsScheme().primary,
) {
    LinearProgressIndicator(
        progress = progress,
        color = color,
        trackColor = colorsScheme().primaryVariant,
        modifier = modifier,
        drawStopIndicator = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireLinearProgressIndicator() = WireTheme {
    Box(
        modifier = Modifier
            .background(colorsScheme().surface)
            .padding(dimensions().spacing16x)
    ) {
        WireLinearProgressIndicator(progress = { 0.3f })
    }
}
