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
package com.wire.android.ui.common.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun ErrorIcon(
    modifier: Modifier = Modifier,
    contentDescription: String
) {
    Icon(
        modifier = modifier
            .clip(shape = RoundedCornerShape(dimensions().spacing4x))
            .background(color = MaterialTheme.wireColorScheme.error)
            .padding(dimensions().spacing6x),
        painter = painterResource(id = R.drawable.ic_attention),
        contentDescription = contentDescription,
        tint = MaterialTheme.wireColorScheme.onError
    )
}
