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
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

@SuppressLint("ComposePreviewNaming")
@Preview(
    name = "Phone Small • Dark",
    group = "Phones",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 360,
    heightDp = 740
)
@Preview(
    name = "Phone Small • Light",
    group = "Phones",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 360,
    heightDp = 740
)
@Preview(
    name = "Phone Medium • Dark",
    group = "Phones",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 411,
    heightDp = 891
)
@Preview(
    name = "Phone Medium • Light",
    group = "Phones",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 411,
    heightDp = 891
)
@Preview(
    name = "Phone Large • Dark",
    group = "Phones",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 432,
    heightDp = 960
)
@Preview(
    name = "Phone Large • Light",
    group = "Phones",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 432,
    heightDp = 960
)
@Preview(
    name = "Tablet 7\" • Dark",
    group = "Tablets",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 800,
    heightDp = 1280
)
@Preview(
    name = "Tablet 7\" • Light",
    group = "Tablets",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 800,
    heightDp = 1280
)
@Preview(
    name = "Tablet 10\" • Dark",
    group = "Tablets",
    showBackground = true,
    backgroundColor = DARK_BG_COLOR,
    uiMode = UI_MODE_NIGHT_YES,
    widthDp = 1280,
    heightDp = 800
)
@Preview(
    name = "Tablet 10\" • Light",
    group = "Tablets",
    showBackground = true,
    backgroundColor = LIGHT_BG_COLOR,
    uiMode = UI_MODE_NIGHT_NO,
    widthDp = 1280,
    heightDp = 800
)
internal annotation class PreviewMultipleScreens
