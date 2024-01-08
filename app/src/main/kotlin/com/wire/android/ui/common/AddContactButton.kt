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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AddContactButton(
    onIconClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        onClick = { onIconClicked() },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_contact),
                contentDescription = stringResource(R.string.content_description_add_contact),
            )
        },
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false,
        minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAddContactButton() {
    WireTheme {
        AddContactButton(onIconClicked = {})
    }
}
