/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@Composable
fun MediaGalleryScreenTopAppBar(
    title: String,
    onCloseClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = title,
        navigationIconType = NavigationIconType.Close,
        elevation = 0.dp,
        actions = {
            WireSecondaryIconButton(
                onButtonClicked = onOptionsClick,
                iconResource = R.drawable.ic_more,
                contentDescription = R.string.content_description_more_options
            )
        }
    )
}
