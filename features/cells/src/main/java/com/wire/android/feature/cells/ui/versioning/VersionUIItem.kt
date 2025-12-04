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
package com.wire.android.feature.cells.ui.versioning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText

@Composable
fun VersionItem(
    cellVersion: CellVersion,
    modifier: Modifier = Modifier,
    onActionClick: (CellVersion) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
            .background(colorsScheme().surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensions().spacing16x),
        ) {
            Icon(
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing10x,
                    vertical = dimensions().spacing2x,
                ),
                painter = painterResource(R.drawable.ic_undo),
                contentDescription = null,
                tint = colorsScheme().onTertiaryButtonDisabled
            )
            Column(
                modifier = Modifier.padding(
                    start = dimensions().corner2x
                )
            ) {
                val currentLabel = if (cellVersion.isCurrentVersion) {
                    stringResource(R.string.version_history_current_label_for_title)
                } else ""

                Text(
                    text = "${cellVersion.modifiedAt} $currentLabel",
                    style = typography().title02,
                    color = colorsScheme().onSurface,
                )
                Row {
                    Text(
                        text = "${cellVersion.modifiedBy} · ${cellVersion.fileSize}",
                        style = typography().label04,
                        color = colorsScheme().secondaryText,
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = colorsScheme().secondaryText,
            modifier = Modifier
                .padding(end = dimensions().spacing16x)
                .clickable(
                    onClick = { onActionClick(cellVersion) },
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = false,
                        radius = dimensions().spacing24x,
                        color = Color.Transparent
                    )
                )
                .then(Modifier.size(dimensions().spacing24x))
        )
    }
}

@Composable
fun VersionTimeHeaderItem(
    timeHeader: UIText,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorsScheme().background)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions().spacing16x,
                    top = dimensions().spacing8x,
                    bottom = dimensions().spacing8x,
                ),
            text = timeHeader.asString(),
            style = typography().title03,
            color = colorsScheme().secondaryText,
        )
    }
}

@MultipleThemePreviews
@Composable
fun VersionItemPreview() {
    WireTheme {
        VersionItem(
            cellVersion = CellVersion(
                versionId = "id",
                modifiedAt = "1:46 PM",
                modifiedBy = "Deniz Agha",
                fileSize = "200MB"
            )
        )
    }
}

@MultipleThemePreviews
@Composable
fun VersionTimeHeaderItemPreview() {
    WireTheme {
        VersionTimeHeaderItem(
            timeHeader = "Today, 3 Dec 2025".toUIText()
        )
    }
}
