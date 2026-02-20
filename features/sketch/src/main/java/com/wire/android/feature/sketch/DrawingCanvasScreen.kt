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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.feature.sketch.model.DrawingCanvasNavArgs
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.feature.sketch.util.PreviewMultipleThemes
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.annotation.features.sketch.WireSketchDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import kotlinx.coroutines.launch

@WireSketchDestination(
    start = true,
    style = PopUpNavigationAnimation::class,
    navArgs = DrawingCanvasNavArgs::class,
)
@Composable
fun DrawingCanvasScreen(
    drawingCanvasNavArgs: DrawingCanvasNavArgs,
    resultNavigator: ResultBackNavigator<DrawingCanvasNavBackArgs>,
    viewModel: DrawingCanvasViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discardDrawing: () -> Unit = remember {
        {
            viewModel.initializeCanvas()
            resultNavigator.navigateBack()
        }
    }
    val onDismissEvent: () -> Unit = remember {
        {
            if (viewModel.state.paths.isNotEmpty()) {
                viewModel.onShowConfirmationDialog()
            } else {
                discardDrawing()
            }
        }
    }
    DrawingCanvasContent(
        state = viewModel.state,
        title = drawingCanvasNavArgs.conversationName,
        onStartDrawingEvent = viewModel::onStartDrawingEvent,
        onDrawEvent = viewModel::onDrawEvent,
        onStopDrawingEvent = viewModel::onStopDrawingEvent,
        onSizeChanged = viewModel::onSizeChanged,
        onStartDrawing = viewModel::onStartDrawing,
        onDraw = viewModel::onDraw,
        onColorChanged = viewModel::onColorChanged,
        onStopDrawing = viewModel::onStopDrawing,
        onDismissEvent = onDismissEvent,
        onUndoStroke = viewModel::onUndoLastStroke,
        onSendSketch = remember {
            {
                scope.launch {
                    resultNavigator.setResult(DrawingCanvasNavBackArgs(viewModel.saveImage(context)))
                    resultNavigator.navigateBack()
                }
            }
        },
    )

    if (viewModel.state.showConfirmationDialog) {
        DrawingDiscardConfirmationDialog(discardDrawing, viewModel::onHideConfirmationDialog)
    }

    BackHandler {
        onDismissEvent()
    }
}

@Composable
internal fun DrawingCanvasContent(
    state: DrawingState,
    title: String,
    onStartDrawingEvent: () -> Unit,
    onDrawEvent: () -> Unit,
    onStopDrawingEvent: () -> Unit,
    onSizeChanged: (Size) -> Unit,
    onStartDrawing: (Offset) -> Unit,
    onDraw: (Offset) -> Unit,
    onColorChanged: (Color) -> Unit,
    onSendSketch: () -> Unit,
    onStopDrawing: () -> Unit,
    onDismissEvent: () -> Unit,
    onUndoStroke: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        topBar = {
            DrawingTopBar(
                conversationTitle = title,
                dismissAction = onDismissEvent,
                onUndoStroke = onUndoStroke,
                state = state
            )
        },
        content = { internalPadding ->
            Column(
                modifier = modifier.padding(internalPadding)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
                ) {
                    DrawingCanvasComponent(
                        state = state,
                        onStartDrawingEvent = onStartDrawingEvent,
                        onDrawEvent = onDrawEvent,
                        onStopDrawingEvent = onStopDrawingEvent,
                        onSizeChanged = onSizeChanged,
                        onStartDrawing = onStartDrawing,
                        onDraw = onDraw,
                        onStopDrawing = onStopDrawing
                    )
                }
                DrawingToolbar(
                    state = state,
                    onColorChanged = onColorChanged,
                    onSendSketch = onSendSketch
                )
            }
        }
    )
}

@Composable
internal fun DrawingTopBar(
    conversationTitle: String,
    dismissAction: () -> Unit,
    onUndoStroke: () -> Unit,
    state: DrawingState
) {
    WireCenterAlignedTopAppBar(
        title = conversationTitle,
        navigationIconType = NavigationIconType.Close(),
        onNavigationPressed = dismissAction,
        actions = {
            WireSecondaryIconButton(
                onButtonClicked = onUndoStroke,
                iconResource = R.drawable.ic_undo,
                contentDescription = R.string.content_description_undo_button,
                state = if (state.paths.isNotEmpty()) WireButtonState.Default else WireButtonState.Disabled,
                minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
            )
        }
    )
}

@Composable
internal fun DrawingToolbar(
    state: DrawingState,
    onColorChanged: (Color) -> Unit,
    onSendSketch: () -> Unit = {},
) {
    val sheetState = rememberWireModalSheetState<Unit>()
    Row(
        Modifier
            .height(dimensions().spacing80x)
            .padding(horizontal = dimensions().spacing8x)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WireSecondaryButton(
            onClick = sheetState::show,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_outline),
                    contentDescription = null,
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
        onColorSelected = remember {
            {
                onColorChanged(it)
                sheetState.hide()
            }
        }
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewDrawingCanvasScreen() = WireTheme {
    DrawingCanvasContent(DrawingState(), "Title", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}
