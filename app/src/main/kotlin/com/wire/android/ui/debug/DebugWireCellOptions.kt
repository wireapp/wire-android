/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun DebugWireCellOptions(
    isCellFeatureEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(modifier = modifier) {

        FolderHeader("Wire Cells")

        RowItemTemplate(
            title = {
                Text(
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = "Enable Wire Cell",
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            },
            actions = {
                WireSwitch(
                    checked = isCellFeatureEnabled,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .padding(end = dimensions().spacing8x)
                        .size(
                            width = dimensions().buttonSmallMinSize.width,
                            height = dimensions().buttonSmallMinSize.height
                        )
                )
            }
        )

    }

}

@PreviewMultipleThemes
@Composable
private fun PreviewDebugWireCellOptions() {
    WireTheme {
        DebugWireCellOptions(
            isCellFeatureEnabled = true,
            onCheckedChange = {},
        )
    }
}
