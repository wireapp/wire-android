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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.SplashBackgroundLayout
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun NewLoginContainer(
    title: String = "",
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    content: @Composable () -> Unit
) {
    NewLoginContent(title, canNavigateBack, onNavigateBack, content)
}

@Composable
private fun NewLoginContent(
    title: String = "",
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit = { }
) {
    NavigationBarBackground()
    WireScaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                modifier = Modifier
                    .clip(WireBottomSheetDefaults.WireBottomSheetShape)
                    .background(WireBottomSheetDefaults.WireSheetContainerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions().spacing16x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.clickable(onClick = onNavigateBack)
                        )
                    }
                    if (title.isBlank().not()) {
                        Text(
                            text = title,
                            style = MaterialTheme.wireTypography.body01,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.size(dimensions().spacing8x))
                }
                Column {
                    VerticalSpace.x16()
                    content()
                }
            }
        }) { _ -> }
}

@Composable
private fun NavigationBarBackground() = Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().background)
            .navigationBarsPadding()
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginContent() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        SplashBackgroundLayout {
            NewLoginContent("Enter your password to log in", true, {}) {
                Text(
                    text = "EMPTY",
                    modifier = Modifier.padding(dimensions().spacing24x)
                )
            }
        }
    }
}
