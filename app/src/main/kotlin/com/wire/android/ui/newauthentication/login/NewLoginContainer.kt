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
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun NewLoginContainer(
    header: @Composable () -> Unit = {},
    contentPadding: Dp = dimensions().spacing24x,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    NavigationBarBackground()
    WireScaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                modifier = Modifier
                    .clip(WireBottomSheetDefaults.WireBottomSheetShape)
                    .background(WireBottomSheetDefaults.WireSheetContainerColor)
            ) {
                Surface(
                    color = WireBottomSheetDefaults.WireSheetContainerColor,
                    shadowElevation = scrollState.rememberTopBarElevationState().value,
                ) {
                    header()
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(start = contentPadding, end = contentPadding, bottom = contentPadding)
                ) {
                    content()
                }
            }
        }) { _ -> }
}

@Composable
fun NewLoginHeader(
    title: String,
    canNavigateBack: Boolean = false,
    contentPadding: Dp = dimensions().spacing24x,
    onNavigateBack: () -> Unit = {},
) {
    val iconClickablePadding = dimensions().spacing12x
    val iconClickableWidth = dimensions().spacing48x
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = iconClickableWidth - iconClickablePadding.times(2) + contentPadding.times(2))
            .padding(horizontal = contentPadding - iconClickablePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canNavigateBack) {
            NavigationIconButton(
                iconType = NavigationIconType.Back(),
                onClick = onNavigateBack,
                modifier = Modifier
                    .width(iconClickableWidth)
            )
        }
        if (title.isBlank().not()) {
            Text(
                text = title,
                style = MaterialTheme.wireTypography.body01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = iconClickablePadding, vertical = contentPadding)
                    .weight(1f)
            )
        }
        if (canNavigateBack) {
            Box(modifier = Modifier.width(iconClickableWidth)) // so that the title is centered when there is a navigation button
        }
    }
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
private fun PreviewNewLoginHeader() = WireTheme {
    NewLoginHeader("Enter your password to log in", true) {}
}


@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginContent() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            NewLoginContainer(
                header = {
                    NewLoginHeader("Enter your password to log in", true) {}
                },
                content = {
                    Text(text = "EMPTY")
                }
            )
        }
    }
}
