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
package com.wire.android.util.ui

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireTheme

const val LIGHT_BG_COLOR = 0xFFEDEFF0
const val DARK_BG_COLOR = 0xFF17181A

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Dark theme",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Light theme",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
)
/**
 * Helper annotation that adds a preview for Light and Dark theme previews, _i.e._
 * with [Preview.uiMode] set to [UI_MODE_NIGHT_NO] and [UI_MODE_NIGHT_YES].
 * It has hardcoded background colors following the [WireColorScheme].
 *
 * **Important**
 *
 * Just like regular [Preview] annotations, it's important that the composable
 * preview is actually reactive to the change in theme. So it might be necessary
 * to wrap the preview in a [WireTheme] block.
 *
 * There's a problem with lint handling custom preview annotations in other modules, so for now it's added to each module.
 */
internal annotation class PreviewMultipleThemes

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Portrait Dark theme",
    group = "Portrait",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 450,
    heightDp = 800,
)
@Preview(
    name = "Portrait Light",
    group = "Portrait",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 450,
    heightDp = 800,
)
internal annotation class PreviewMultipleThemesForPortrait

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Landscape Dark theme",
    group = "Landscape",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 800,
    heightDp = 450,
)
@Preview(
    name = "Landscape Light theme",
    group = "Landscape",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 800,
    heightDp = 450,
)
internal annotation class PreviewMultipleThemesForLandscape

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Square Dark theme",
    group = "Square",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 600,
    heightDp = 600,
)
@Preview(
    name = "Square Light theme",
    group = "Square",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 600,
    heightDp = 600,
)
internal annotation class PreviewMultipleThemesForSquare
