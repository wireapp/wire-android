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
package com.wire.android.ui.common.remove

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoveIcon(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Icon(
        modifier = modifier
            .size(dimensions().spacing24x)
            .combinedClickable(onClick = onClick)
            .clip(shape = CircleShape)
            .background(color = MaterialTheme.wireColorScheme.inverseSurface)
            .padding(dimensions().spacing6x),
        painter = painterResource(id = R.drawable.ic_close),
        contentDescription = contentDescription,
        tint = MaterialTheme.wireColorScheme.inverseOnSurface
    )
}
