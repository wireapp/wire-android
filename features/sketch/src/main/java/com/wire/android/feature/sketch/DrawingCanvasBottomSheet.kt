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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    onDismissSketch: () -> Unit,
    onSendSketch: (Uri) -> Unit,
    tempWritableImageUri: Uri?,
    conversationTitle: String = "",
    viewModel: DrawingCanvasViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })
    val onDismissEvent: () -> Unit = remember {
        {
            if (viewModel.state.paths.isNotEmpty()) {
                viewModel.onShowConfirmationDialog()
            } else {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissSketch() }
            }
        }
    }
    val dismissEvent: () -> Unit = remember {
        {
            viewModel.initializeCanvas()
            onDismissSketch()
        }
    }

    ModalBottomSheet(
        shape = CutCornerShape(dimensions().spacing0x),
        containerColor = colorsScheme().background,
        dragHandle = {
            DrawingTopBar(conversationTitle, onDismissEvent, viewModel::onUndoLastStroke, viewModel.state)
        },
        sheetState = sheetState,
        onDismissRequest = onDismissEvent,
        properties = ModalBottomSheetProperties(
            isFocusable = true,
            securePolicy = SecureFlagPolicy.SecureOn,
            shouldDismissOnBackPress = false
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true)
        ) {
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
        DrawingToolbar(
            state = viewModel.state,
            onColorChanged = viewModel::onColorChanged,
            onSendSketch = {
                scope.launch { onSendSketch(viewModel.saveImage(context, tempWritableImageUri)) }
                    .invokeOnCompletion { scope.launch { sheetState.hide() } }
            }
        )
    }

    if (viewModel.state.showConfirmationDialog) {
        DiscardDialogConfirmation(scope, sheetState, dismissEvent, viewModel::onHideConfirmationDialog)
    }
}

@Composable
internal fun DrawingTopBar(
    conversationTitle: String,
    dismissAction: () -> Unit,
    onUndoStroke: () -> Unit,
    state: DrawingState
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing8x),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WireTertiaryIconButton(
            onButtonClicked = dismissAction,
            iconResource = R.drawable.ic_close,
            contentDescription = R.string.content_description_close_button,
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
        Text(
            text = conversationTitle,
            style = MaterialTheme.wireTypography.title01,
            modifier = Modifier.align(Alignment.CenterVertically),
            maxLines = MAX_LINES_TOPBAR,
            overflow = TextOverflow.Ellipsis
        )
        WireSecondaryIconButton(
            onButtonClicked = onUndoStroke,
            iconResource = R.drawable.ic_undo,
            contentDescription = R.string.content_description_undo_button,
            state = if (state.paths.isNotEmpty()) WireButtonState.Default else WireButtonState.Disabled,
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawingToolbar(
    state: DrawingState,
    onColorChanged: (Color) -> Unit,
    onSendSketch: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberWireModalSheetState()
    val openColorPickerSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeColorPickerSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }
    Row(
        Modifier
            .height(dimensions().spacing80x)
            .padding(horizontal = dimensions().spacing8x)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WireSecondaryButton(
            onClick = openColorPickerSheet,
            leadingIcon = {
                Icon(
                    Icons.Default.Circle,
                    null,
                    tint = state.currentPath.color,
                    modifier = Modifier
                        .border(
                            shape = CircleShape,
                            border = BorderStroke(dimensions().spacing1x, colorsScheme().secondaryText)
                        )
                )
            },
            leadingIconAlignment = IconAlignment.Center,
            fillMaxWidth = false,
            minSize = dimensions().buttonSmallMinSize,
            minClickableSize = dimensions().buttonMinClickableSize,
            shape = RoundedCornerShape(dimensions().spacing12x),
            contentPadding = PaddingValues(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x),
        )
        Spacer(Modifier.size(dimensions().spacing2x))
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

    DrawingToolPicker(
        sheetState = sheetState,
        currentColor = state.currentPath.color,
        onColorSelected = {
            onColorChanged(it)
            closeColorPickerSheet()
        }
    )
}

private const val MAX_LINES_TOPBAR = 1
