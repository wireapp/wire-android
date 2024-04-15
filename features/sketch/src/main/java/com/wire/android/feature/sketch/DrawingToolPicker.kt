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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun DrawingToolPicker(
    sheetState: WireModalSheetState,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colorPalette = colorsScheme().sketchColorPalette
    WireModalSheetLayout(
        dragHandle = null,
        sheetState = sheetState,
        coroutineScope = scope,
    ) {
        Column(
            modifier = Modifier
                .background(colorsScheme().surface)
                .wrapContentSize()
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = dimensions().spacing12x)
                    .size(width = dimensions().modalBottomSheetDividerWidth, height = dimensions().spacing4x)
                    .background(color = colorsScheme().background, shape = RoundedCornerShape(size = dimensions().spacing2x))

            )
            MenuModalSheetContent(
                header = MenuModalSheetHeader.Visible(title = stringResource(id = R.string.title_color_picker)),
                menuItems = listOf {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .background(colorsScheme().surface)
                            .padding(PaddingValues(horizontal = dimensions().spacing8x)),
                        columns = GridCells.Fixed(GRID_CELLS),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        items(colorPalette.size) { index ->
                            val color = colorPalette[index]
                            ColorOptionButton(
                                color = color,
                                selected = color == currentColor,
                                onColorSelected = { onColorSelected(color) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ColorOptionButton(color: Color, selected: Boolean = false, onColorSelected: () -> Unit) {
    when (selected) {
        true -> SelectedColor(color, onColorSelected)
        false -> NonSelectedColor(color, onColorSelected)
    }
}

@Composable
private fun NonSelectedColor(color: Color, onColorSelected: () -> Unit) {
    OutlinedIconToggleButton(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(dimensions().spacing12x),
        checked = false,
        border = if (color == Color.White) BorderStroke(dimensions().spacing1x, colorsScheme().onBackground) else null,
        colors = IconToggleButtonColors(
            containerColor = color,
            checkedContainerColor = color,
            checkedContentColor = color,
            disabledContainerColor = color,
            disabledContentColor = color,
            contentColor = color,
        ),
        onCheckedChange = { onColorSelected() },
        content = { }
    )
}

@Composable
private fun SelectedColor(color: Color, onColorSelected: () -> Unit) {
    OutlinedIconToggleButton(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .padding(dimensions().spacing12x)
            .background(colorsScheme().surface, CircleShape)
            .border(dimensions().spacing2x, colorsScheme().secondaryText, CircleShape),
        checked = true,
        border = BorderStroke(
            if (color == Color.White) dimensions().spacing1x else dimensions().spacing2x,
            if (color == Color.White) colorsScheme().onSurface else colorsScheme().surface
        ),
        colors = IconToggleButtonColors(
            containerColor = color,
            checkedContainerColor = color,
            checkedContentColor = color,
            disabledContainerColor = color,
            disabledContentColor = color,
            contentColor = color,
        ),
        onCheckedChange = { onColorSelected() }
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (color == Color.White) colorsScheme().onBackground else colorsScheme().background,
            modifier = Modifier.size(
                dimensions().spacing12x
            )
        )
    }
}

private const val GRID_CELLS = 6


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewDrawingToolPicker() {
    DrawingToolPicker(
        sheetState = WireModalSheetState(SheetValue.Expanded),
        currentColor = Color.White,
        onColorSelected = {}
    )
}
