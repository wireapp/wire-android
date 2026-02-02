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

package com.wire.android.ui.common

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.wire.android.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.textfield.WireLabel
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.CreateConversationParam

@Composable
internal fun WireDropDown(
    items: List<String>,
    label: String?,
    modifier: Modifier = Modifier,
    defaultItemIndex: Int = -1,
    selectedItemIndex: Int = defaultItemIndex,
    autoUpdateSelection: Boolean = true,
    showDefaultTextIndicator: Boolean = true,
    leadingCompose: @Composable ((index: Int) -> Unit)? = null,
    onChangeClickDescription: String = stringResource(R.string.content_description_change_it_label),
    placeholder: String = stringResource(R.string.wire_dropdown_placeholder),
    onSelected: (selectedIndex: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember(selectedItemIndex) { mutableStateOf(selectedItemIndex) }
    var selectionFieldWidth by remember { mutableStateOf(Size.Zero) }
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    val selectionText = if (selectedIndex != -1) {
        items[selectedIndex] + LocalContext.current.defaultTextIndicator(
            showDefaultTextIndicator,
            selectedIndex,
            defaultItemIndex
        )
    } else {
        placeholder
    }
    val borderColor = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline
    val shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)

    Column(modifier) {
        label?.let {
            WireLabel(
                labelText = it,
                labelMandatoryIcon = false,
                state = WireTextFieldState.Default,
                interactionSource = remember { MutableInteractionSource() }
            )
        }

        Column(
            modifier = Modifier
                .clip(shape)
                .background(color = MaterialTheme.wireColorScheme.secondaryButtonEnabled, shape = shape)
                .border(width = 1.dp, color = borderColor, shape)
        ) {

            SelectionField(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        // This value is used to assign to
                        // the DropDown the same width
                        selectionFieldWidth = coordinates.size.toSize()
                    }
                    .clickable(onClickLabel = onChangeClickDescription) { expanded = true },
                leadingCompose = leadingCompose,
                selectedIndex = selectedIndex,
                text = selectionText,
                arrowRotation = arrowRotation
            )

            MenuPopUp(
                shape = shape,
                textFieldWidth = selectionFieldWidth,
                expanded = expanded,
                borderColor = borderColor,
                leadingCompose = leadingCompose,
                selectedIndex = selectedIndex,
                items = items,
                showDefaultTextIndicator = showDefaultTextIndicator,
                defaultItemIndex = defaultItemIndex,
                selectionText = selectionText,
                arrowRotation = arrowRotation,
                hidePopUp = { expanded = false },
                onChange = { index ->
                    if (autoUpdateSelection) selectedIndex = index
                    expanded = false
                    onSelected(index)
                }
            )
        }
    }
}

@Composable
private fun MenuPopUp(
    shape: RoundedCornerShape,
    textFieldWidth: Size,
    expanded: Boolean,
    borderColor: Color,
    leadingCompose: @Composable ((index: Int) -> Unit)?,
    selectedIndex: Int,
    items: List<String>,
    showDefaultTextIndicator: Boolean,
    defaultItemIndex: Int,
    selectionText: String,
    arrowRotation: Float,
    hidePopUp: () -> Unit,
    onChange: (selectedIndex: Int) -> Unit
) {
    val dropdownDescription = stringResource(R.string.content_description_drop_down)

    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = shape)) {
        // we want PopUp to cover the selection field, so we set this offset.
        // "- 8.dp" is because DropdownMenu has inner top padding, which can't be changed,
        // so without this additional 8.dp selection text will "jump" while opening/closing menu.
        val popUpTopOffset = with(LocalDensity.current) { -textFieldWidth.height.toDp() - 8.dp }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = hidePopUp,
            offset = DpOffset(0.dp, popUpTopOffset),
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldWidth.width.toDp() })
                .background(color = MaterialTheme.wireColorScheme.secondaryButtonEnabled)
                .border(width = 1.dp, color = borderColor, shape)
                .semantics { paneTitle = dropdownDescription }
        ) {

            SelectionField(
                leadingCompose = leadingCompose,
                selectedIndex = selectedIndex,
                text = selectionText,
                arrowRotation = arrowRotation,
                modifier = Modifier.clickable(onClickLabel = stringResource(R.string.content_description_close_dropdown)) {
                    hidePopUp()
                }
            )

            List(items.size) { index ->
                HorizontalDivider(
                    color = colorsScheme().divider
                )

                DropdownItem(
                    text = items[index] + LocalContext.current.defaultTextIndicator(
                        showDefaultTextIndicator,
                        index,
                        defaultItemIndex
                    ),
                    isSelected = index == selectedIndex,
                    leadingCompose = leadingCompose?.let { { it(index) } },
                    onClick = { onChange(index) }
                )
            }
        }
    }
}

@Composable
private fun SelectionField(
    leadingCompose: @Composable ((index: Int) -> Unit)?,
    selectedIndex: Int,
    text: String,
    arrowRotation: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                top = dimensions().spacing12x,
                bottom = dimensions().spacing12x
            )

    ) {
        leadingCompose?.let {
            LeadingIcon { it(selectedIndex) }
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            text = text,
            style = MaterialTheme.wireTypography.body01,
            color = if (selectedIndex != -1) {
                MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
            } else {
                MaterialTheme.wireColorScheme.secondaryText
            }
        )
        Icon(
            painter = painterResource(commonR.drawable.ic_expand_more),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier
                .padding(top = 4.dp)
                .rotate(arrowRotation)
        )
    }
}

private fun Context.defaultTextIndicator(showDefaultIndicator: Boolean, index: Int, defaultIndex: Int): String {
    val defaultText = getString(R.string.wire_dropdown_default_indicator)
    return if (index == defaultIndex && showDefaultIndicator) defaultText else String.EMPTY
}

@Composable
private fun DropdownItem(
    text: String,
    leadingCompose: (@Composable () -> Unit)?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectLabel = stringResource(R.string.content_description_select_label)
    val closeDropdownLabel = stringResource(R.string.content_description_close_dropdown)
    return DropdownMenuItem(
        text = {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = if (isSelected) MaterialTheme.wireTypography.body02 else MaterialTheme.wireTypography.body01,
                color = if (isSelected) {
                    MaterialTheme.wireColorScheme.onSecondaryButtonSelected
                } else {
                    MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
                }
            )
        },
        leadingIcon = leadingCompose,
        trailingIcon = {
            if (isSelected) {
                WireCheckIcon(contentDescription = R.string.content_description_empty)
            }
        },
        onClick = onClick,
        modifier = Modifier
            .semantics {
                if (isSelected) {
                    selected = true
                    onClick(closeDropdownLabel) { false }
                } else {
                    onClick(selectLabel) { false }
                }
            }
            .background(
                color = if (isSelected) {
                    MaterialTheme.wireColorScheme.secondaryButtonSelected
                } else {
                    MaterialTheme.wireColorScheme.tertiaryButtonEnabled
                }
            )
    )
}

@Composable
private fun RowScope.LeadingIcon(convent: @Composable () -> Unit) {
    Box(
        Modifier
            .align(Alignment.CenterVertically)
            .padding(end = dimensions().spacing8x)
    ) {
        convent()
    }
}

@Composable
@Preview
fun PreviewWireDropdownPreviewWithLabel() {
    WireDropDown(
        items = CreateConversationParam.Protocol.entries.map { it.name },
        defaultItemIndex = 0,
        selectedItemIndex = 0,
        label = "Protocol",
        modifier = Modifier
    ) {}
}
