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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CellFileCard(
    file: CellFile,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    text = file.fileName
                )
                AnimatedVisibility(file.uploadProgress == null) {
                    Icon(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(8.dp)
                            .clickable { onClickDelete() },
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.LightGray,
                    )
                }
            }

            AnimatedVisibility(file.uploadProgress != null) {

                val progress = remember { Animatable(0f) }

                LaunchedEffect(file.uploadProgress) {
                    progress.animateTo(
                        targetValue = file.uploadProgress ?: 0f,
                        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                    )
                }


                WireLinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellCard() {
    WireTheme {
        CellFileCard(
            file = CellFile(
                fileName = "file name",
                uploadProgress = 0.5f
            ),
            onClickDelete = {}
        )
    }
}
