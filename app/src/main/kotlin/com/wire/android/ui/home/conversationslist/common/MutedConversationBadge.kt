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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun MutedConversationBadge(onClick: () -> Unit) {
    Box(modifier = Modifier
        .width(dimensions().spacing24x)
        .height(dimensions().spacing18x)) {
        WireSecondaryButton(
            onClick = onClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mute),
                    contentDescription = stringResource(R.string.content_description_muted_conversation),
                    modifier = Modifier.size(dimensions().spacing12x),
                    tint = colorsScheme().onSecondaryButtonEnabled
                )
            },
            fillMaxWidth = false,
            minHeight = dimensions().badgeSmallMinSize.height,
            minWidth = dimensions().badgeSmallMinSize.width,
            shape = RoundedCornerShape(size = dimensions().spacing6x),
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview
@Composable
fun PreviewMutedConversationBadge() {
    MutedConversationBadge {}
}
