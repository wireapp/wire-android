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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.DEFAULT_WEIGHT
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@Composable
fun SelectableMenuBottomSheetItem(
    title: String,
    titleColor: Color? = null,
    titleStyleUnselected: TextStyle = MaterialTheme.wireTypography.body02,
    titleStyleSelected: TextStyle = MaterialTheme.wireTypography.body02,
    subLine: String? = null,
    icon: @Composable () -> Unit = { },
    onItemClick: Clickable = Clickable(enabled = false) {},
    state: RichMenuItemState = RichMenuItemState.DEFAULT
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .defaultMinSize(minHeight = dimensions().spacing48x)
            .let { if (isSelectedItem(state)) it.background(MaterialTheme.wireColorScheme.secondaryButtonSelected) else it }
            .clickable(onItemClick)
            .padding(vertical = dimensions().spacing12x, horizontal = dimensions().spacing16x)
    ) {
        icon()
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(DEFAULT_WEIGHT),
        ) {
            MenuItemHeading(
                title = title, color = titleColor,
                titleStyleUnselected = titleStyleUnselected,
                titleStyleSelected = titleStyleSelected,
                state = state
            )
            if (subLine != null) {
                MenuItemSubLine(
                    subLine = subLine,
                    modifier = Modifier.padding(top = dimensions().spacing8x)
                )
            }
        }
        if (isSelectedItem(state)) {
            Column(
                modifier = Modifier
                    .padding(start = dimensions().spacing8x)
                    .align(Alignment.CenterVertically)
            ) {
                WireCheckIcon()
            }
        }
    }
}

@PackagePrivate
@Composable
fun MenuItemHeading(
    title: String,
    titleStyleUnselected: TextStyle = MaterialTheme.wireTypography.body02,
    titleStyleSelected: TextStyle = MaterialTheme.wireTypography.body02,
    state: RichMenuItemState = RichMenuItemState.DEFAULT,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Text(
        style = if (isSelectedItem(state)) titleStyleSelected else titleStyleUnselected,
        color = if (isSelectedItem(state)) MaterialTheme.wireColorScheme.primary else color ?: MaterialTheme.wireColorScheme.onBackground,
        text = title,
        modifier = modifier.fillMaxWidth()
    )
}

@PackagePrivate
@Composable
fun MenuItemSubLine(
    subLine: String,
    modifier: Modifier = Modifier
) {
    Text(
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.labelText,
        text = subLine,
        modifier = modifier.fillMaxWidth()
    )
}

private fun isSelectedItem(state: RichMenuItemState) = state == RichMenuItemState.SELECTED

enum class RichMenuItemState {
    DEFAULT, SELECTED
}

@Composable
@Preview
fun PreviewRichMenuBottomSheetItem() {
    SelectableMenuBottomSheetItem(
        title = "title",
        titleColor = null,
        subLine = "subLine",
        onItemClick = Clickable {},
        state = RichMenuItemState.SELECTED
    )
}
