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

package com.wire.android.ui.common.topappbar

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.common.R

@Composable
fun NavigationIconButton(iconType: NavigationIconType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = iconType.icon, contentDescription = stringResource(iconType.contentDescription))
    }
}

@Composable
fun BackNavigationIconButton(onBackButtonClick: () -> Unit) {
    NavigationIconButton(NavigationIconType.Back(), onBackButtonClick)
}

sealed class NavigationIconType(val icon: ImageVector, @StringRes open val contentDescription: Int) {
    data class Back(@StringRes override val contentDescription: Int = R.string.content_description_back_button) :
        NavigationIconType(Icons.AutoMirrored.Filled.ArrowBack, contentDescription)

    data class Close(@StringRes override val contentDescription: Int = R.string.content_description_close_button) :
        NavigationIconType(Icons.Filled.Close, contentDescription)

    data object Menu : NavigationIconType(Icons.Filled.Menu, R.string.content_description_menu_button)
    data object Collapse : NavigationIconType(Icons.Filled.KeyboardArrowDown, R.string.content_description_drop_down_icon)
}
