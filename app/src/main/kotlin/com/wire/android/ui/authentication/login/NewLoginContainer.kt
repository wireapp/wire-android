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
package com.wire.android.ui.authentication.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.MainBackgroundComponent
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.SetStatusBarColorForWavesBackground
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun NewLoginContainer(
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    content: @Composable () -> Unit
) {
    NewLoginContent(canNavigateBack, onNavigateBack, content)
}

@Composable
private fun NewLoginContent(
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = { }
) {
    WireScaffold(topBar = { SetStatusBarColorForWavesBackground() }) { internalPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainBackgroundComponent()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topEnd = dimensions().spacing8x, topStart = dimensions().spacing8x))
                    .align(Alignment.BottomCenter)
                    .background(colorsScheme().background)
                    .padding(dimensions().spacing16x)
            ) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(dimensions().buttonCircleMinSize)
                    ) {
                        Icon(
                            painter = rememberVectorPainter(image = Icons.Filled.ArrowBack),
                            contentDescription = stringResource(id = R.string.content_description_back_button),
                            tint = MaterialTheme.wireColorScheme.onBackground,
                        )
                    }
                } else {
                    VerticalSpace.x16()
                }
                content()
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginContent() = WireTheme {
    NewLoginContent(true, {}) { Text(text = "EMPTY") }
}
