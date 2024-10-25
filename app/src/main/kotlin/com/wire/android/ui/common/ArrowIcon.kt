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

package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun ArrowRightIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescription: Int = R.string.content_description_right_arrow,
) {
    ArrowIcon(
        arrowIcon = R.drawable.ic_arrow_right,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
fun ArrowLeftIcon(
    modifier: Modifier = Modifier
) {
    ArrowIcon(
        arrowIcon = R.drawable.ic_arrow_left,
        contentDescription = R.string.content_description_left_arrow,
        modifier = modifier
    )
}

@Composable
private fun ArrowIcon(
    @DrawableRes arrowIcon: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = arrowIcon),
        contentDescription = stringResource(contentDescription),
        modifier = modifier
            .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            .then(modifier)
    )
}
