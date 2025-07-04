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
package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun PageLoadingIndicator(
    text: String,
    modifier: Modifier = Modifier,
    prefixIconResId: Int? = null,
) {
    Surface(
        color = colorsScheme().surfaceVariant,
        shape = RoundedCornerShape(dimensions().corner10x),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing4x
                ),
        ) {
            prefixIconResId?.let { iconResId ->
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = colorsScheme().secondaryText,
                    modifier = Modifier
                        .padding(end = dimensions().spacing4x)
                        .size(dimensions().spacing10x),
                )
            }
            Text(
                text = text,
                style = typography().subline01,
                color = colorsScheme().secondaryText,
            )
        }
    }
}

@Composable
private fun PreviewWrapper(content: @Composable () -> Unit) = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .fillMaxWidth()
        .background(colorsScheme().surfaceContainerLow)
        .padding(dimensions().spacing16x),
) {
    content()
}

@PreviewMultipleThemes
@Composable
fun PreviewPageLoadingIndicator_Loading() = WireTheme {
    PreviewWrapper {
        PageLoadingIndicator(
            text = stringResource(R.string.conversation_history_loaded),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewPageLoadingIndicator_AllLoaded() = WireTheme {
    PreviewWrapper {
        PageLoadingIndicator(
            text = stringResource(R.string.conversation_history_loading),
            prefixIconResId = R.drawable.ic_undo,
        )
    }
}
