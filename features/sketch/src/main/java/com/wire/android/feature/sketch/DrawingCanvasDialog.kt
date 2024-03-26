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
package com.wire.android.feature.sketch

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvas(
    viewModel: DrawingCanvasViewModel = viewModel(),
    onDismissSketch: () -> Unit,
    onSendSketch: () -> Unit,
    tempWritableImageUri: Uri?,

    ) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 30.dp)
        .clickable { showBottomSheet = true }
    ) {
        ModalBottomSheet(
            dragHandle = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            onDismissSketch()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(
                                com.google.android.material.R.string.mtrl_picker_cancel
                            )
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveImage(tempWritableImageUri)
                            onSendSketch()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = stringResource(
                                com.google.android.material.R.string.mtrl_picker_cancel
                            )
                        )
                    }

                }
            },
            sheetState = sheetState,
            onDismissRequest = {
                showBottomSheet = false
                onDismissSketch()
            }
        ) {
            DrawingCanvasComponent(viewModel)
        }
    }
}
