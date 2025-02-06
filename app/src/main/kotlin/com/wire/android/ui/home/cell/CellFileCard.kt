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
package com.wire.android.ui.home.cell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.cells.CellNode

@Composable
fun CellFileCard(
    file: CellNodeUi,
    onClick: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    width = 1.dp,
                    color = colorsScheme().secondaryButtonEnabledOutline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.height(74.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        tint = colorsScheme().onSurface,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
                    if (file.node.isDraft) {
                        Text(
                            modifier = Modifier
                                .background(
                                    color = colorsScheme().onPrimaryVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp),
                            text = "DRAFT",
                            color = colorsScheme().inverseOnSurface,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.fillMaxHeight().weight(1f))
                Text(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                    style = typography().body02,
                    color = colorsScheme().onSurface,
                    fontSize = 14.sp,
                    maxLines = 2,
                    text = file.fileName,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(file.uploadProgress != null) {
                    WireLinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { file.uploadProgress ?: 0f },
                        color = if (file.uploadError) colorsScheme().error else colorsScheme().primary
                    )
                }
            }
        }
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .clickable { onClickDelete() }
                .background(
                    color = colorsScheme().surface,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = colorsScheme().secondaryButtonEnabledOutline,
                    shape = CircleShape
                )
                .padding(4.dp),
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = colorsScheme().onSurface,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellCard() {
    WireTheme {
        CellFileCard(
            file = CellNodeUi(
                node = CellNode(
                    uuid = "uuid",
                    path = "path",
                    versionId = "",
                ),
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus",
                uploadProgress = 0.5f
            ),
            onClick = {},
            onClickDelete = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellCardError() {
    WireTheme {
        CellFileCard(
            file = CellNodeUi(
                node = CellNode(
                    uuid = "uuid",
                    path = "path",
                    versionId = "",
                    isDraft = true,
                ),
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus",
                uploadProgress = 0.5f,
                uploadError = true,
            ),
            onClick = {},
            onClickDelete = {},
        )
    }
}
