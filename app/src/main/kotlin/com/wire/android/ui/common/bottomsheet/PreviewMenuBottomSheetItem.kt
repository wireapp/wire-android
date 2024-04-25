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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Preview
@Composable
fun PreviewMenuBottomSheetItem() {
    MenuBottomSheetItem(
        title = "very long looooooong title",
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_erase,
                contentDescription = "",
            )
        },
        action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "very long looooooong action",
                    style = MaterialTheme.wireTypography.body01,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                Spacer(modifier = Modifier.size(dimensions().spacing16x))
                ArrowRightIcon()
            }
        }
    )
}
