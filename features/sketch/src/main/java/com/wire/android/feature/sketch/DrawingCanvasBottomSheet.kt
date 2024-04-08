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

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.feature.sketch.config.DrawingViewModelFactory
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.feature.sketch.tools.DrawingToolsConfig
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    drawingToolsConfig: DrawingToolsConfig,
    onDismissSketch: () -> Unit,
    onSendSketch: (Uri) -> Unit,
    tempWritableImageUri: Uri?,
    viewModel: DrawingCanvasViewModel = viewModel(factory = DrawingViewModelFactory(drawingToolsConfig)),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        containerColor = colorsScheme().background,
        dragHandle = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .disableDrag(),
            ) {
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissSketch() }
                    },
                ) {
                    Icon(Icons.Default.Close, null)
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(Icons.AutoMirrored.Default.Undo, null)
                }
            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissSketch
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(Modifier.weight(weight = 1f, fill = true)) {
                DrawingCanvasComponent(
                    state = viewModel.state,
                    onStartDrawingEvent = viewModel::onStartDrawingEvent,
                    onDrawEvent = viewModel::onDrawEvent,
                    onStopDrawingEvent = viewModel::onStopDrawingEvent,
                    onSizeChanged = viewModel::onSizeChanged,
                    onStartDrawing = viewModel::onStartDrawing,
                    onDraw = viewModel::onDraw,
                    onStopDrawing = viewModel::onStopDrawing
                )
            }
            Row(
                Modifier
                    .height(dimensions().spacing80x)
                    .fillMaxWidth()
            ) {
                DrawingToolbar(
                    state = viewModel.state,
                    onSendSketch = {
                        scope.launch { onSendSketch(viewModel.saveImage(context, tempWritableImageUri)) }
                            .invokeOnCompletion { scope.launch { sheetState.hide() } }
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawingToolbar(
    state: DrawingState,
    onSendSketch: () -> Unit = {},
) {
    Row(
        Modifier
            .disableDrag()
            .fillMaxHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Default.Circle, null)
        }
        WirePrimaryIconButton(
            onButtonClicked = onSendSketch,
            iconResource = R.drawable.ic_send,
            contentDescription = R.string.content_description_send_button,
            state = if (state.paths.isNotEmpty()) WireButtonState.Default else WireButtonState.Disabled,
            shape = RoundedCornerShape(dimensions().spacing20x),
            colors = wireSendPrimaryButtonColors(),
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
    }
}

/**
 * Disables the drag gesture on the bottom sheet.
 * This is a compromise made for the first iteration of the feature.
 *
 * TODO: The final implementation should be a full screen drawing experience.
 */
@SuppressLint("ModifierFactoryUnreferencedReceiver")
private fun Modifier.disableDrag() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(pass = PointerEventPass.Initial).changes.forEach {
                val offset = it.positionChange()
                if (abs(offset.y) > 0f) {
                    it.consume()
                }
            }
        }
    }
}
