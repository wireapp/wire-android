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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MutedConversationBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(dimensions().spacing24x)
            .height(dimensions().spacing20x)
            .padding(PaddingValues(dimensions().spacing0x))
            .clip(shape = RoundedCornerShape(size = dimensions().spacing6x))
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.outline,
                shape = RoundedCornerShape(dimensions().spacing6x)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_mute),
            contentDescription = stringResource(R.string.content_description_muted_conversation),
            modifier = Modifier.size(dimensions().spacing12x),
            tint = colorsScheme().onSecondaryButtonEnabled
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMutedConversationBadge() = WireTheme {
    MutedConversationBadge()
}
