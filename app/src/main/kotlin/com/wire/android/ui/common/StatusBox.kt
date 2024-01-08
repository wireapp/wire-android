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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

/**
 * Outlined box with a text inside.
 * Used for things like "Deleted" users,
 * and "Deleted message" or "Edited message"
 */
@Composable
fun StatusBox(
    statusText: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    badgeColor: Color = MaterialTheme.wireColorScheme.surfaceVariant,
    withBorder: Boolean = true,
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(size = dimensions().spacing4x))
            .background(badgeColor)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (withBorder) {
                        MaterialTheme.wireColorScheme.outline
                    } else {
                        badgeColor
                    }
                ),
                shape = RoundedCornerShape(size = dimensions().spacing4x),
            )
            .padding(
                horizontal = dimensions().spacing4x,
                vertical = dimensions().spacing2x

            )
    ) {
        Text(
            text = statusText,
            style = typography().label03.copy(color = textColor)
        )
    }
}

@Composable
fun DeletedLabel(modifier: Modifier = Modifier) {
    StatusBox(
        statusText = stringResource(id = R.string.label_user_deleted),
        modifier = modifier
    )
}

@Composable
fun ProtocolLabel(
    protocolName: String,
    modifier: Modifier = Modifier
) {
    StatusBox(
        statusText = protocolName,
        modifier = modifier,
        textColor = MaterialTheme.wireColorScheme.onPrimary,
        badgeColor = MaterialTheme.wireColorScheme.primary,
        withBorder = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewDeletedLabel() {
    DeletedLabel()
}

@PreviewMultipleThemes
@Composable
fun PreviewProtocolLabel() {
    ProtocolLabel("MLS")
}
