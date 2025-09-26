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
package com.wire.android.feature.meetings.ui.util

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireTheme

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Dark theme",
    showBackground = true,
    backgroundColor = 0xFF17181A,
    uiMode = UI_MODE_NIGHT_YES
)
@Preview(
    name = "Light theme",
    showBackground = true,
    backgroundColor = 0xFFEDEFF0,
    uiMode = UI_MODE_NIGHT_NO
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
