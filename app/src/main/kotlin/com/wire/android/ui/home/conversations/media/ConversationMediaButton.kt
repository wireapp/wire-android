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
 */
package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions

@Composable
fun ConversationMediaButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
        WireSecondaryButton(
            modifier = modifier,
            text = stringResource(R.string.label_conversation_media),
            onClick = onClick,
            minSize = DpSize(dimensions().spacing0x, dimensions().spacing48x),
            fillMaxWidth = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = stringResource(R.string.label_conversation_media),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = dimensions().spacing8x)
                )
            }
        )
}
