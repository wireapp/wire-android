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
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.wire.android.R
import com.wire.android.ui.common.textfield.Label
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.ConversationOptions

@Composable
internal fun WireDropDown(
    items: List<String>,
    defaultItemIndex: Int = -1,
    label: String?,
    modifier: Modifier,
    autoUpdateSelection: Boolean = true,
    showDefaultTextIndicator: Boolean = true,
    leadingCompose: @Composable ((index: Int) -> Unit)? = null,
    onSelected: (selectedIndex: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember(defaultItemIndex) { mutableStateOf(defaultItemIndex) }
    var selectionFieldWidth by remember { mutableStateOf(Size.Zero) }
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    val selectionText = if (selectedIndex != -1) {
        items[selectedIndex] + LocalContext.current.defaultTextIndicator(
            showDefaultTextIndicator,
            selectedIndex,
            defaultItemIndex
        )
    } else stringResource(R.string.wire_dropdown_placeholder)
    val borderColor = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline
    val shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)


    Column(modifier) {
        label?.let {
            Label(it, false, WireTextFieldState.Default, remember { MutableInteractionSource() }, wireTextFieldColors())
        }

        Column(modifier = Modifier
            .clip(shape)
            .background(color = MaterialTheme.wireColorScheme.secondaryButtonEnabled, shape = shape)
            .border(width = 1.dp, color = borderColor, shape)
        ) {

            SelectionField(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    // This value is used to assign to
                    // the DropDown the same width
                    selectionFieldWidth = coordinates.size.toSize()
                }
                    .clickable { expanded = true },
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
        ) {

            SelectionField(
                Modifier.clickable { hidePopUp() },
                leadingCompose,
                selectedIndex,
                selectionText,
                arrowRotation
            )

            List(items.size) { index ->
                Divider()

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
    modifier: Modifier = Modifier,
    leadingCompose: @Composable ((index: Int) -> Unit)?,
    selectedIndex: Int,
    text: String,
    arrowRotation: Float
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
            color = if (selectedIndex != -1) MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
            else MaterialTheme.wireColorScheme.secondaryText
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = stringResource(R.string.change),
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
) = DropdownMenuItem(
    onClick,
    Modifier.background(
        color = if (isSelected) MaterialTheme.wireColorScheme.secondaryButtonSelected
        else MaterialTheme.wireColorScheme.tertiaryButtonEnabled
    )
) {
    leadingCompose?.let {
        LeadingIcon{ it() }
    }

    Text(
        text = text,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        style = if (isSelected) MaterialTheme.wireTypography.body02 else MaterialTheme.wireTypography.body01,
        color = if (isSelected) MaterialTheme.wireColorScheme.onSecondaryButtonSelected
        else MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
    )
    if (isSelected) {
        WireCheckIcon()
    }
}

@Composable
private fun RowScope.LeadingIcon(convent: @Composable () -> Unit) {
        Box(
            Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensions().spacing8x)) {
            convent()
        }
}

@Composable
@Preview
private fun WireDropdownPreviewWithLabel() {
    WireDropDown(
        items = ConversationOptions.Protocol.values().map { it.name }, defaultItemIndex = 0, "Protocol", modifier = Modifier
    ) {}
}
