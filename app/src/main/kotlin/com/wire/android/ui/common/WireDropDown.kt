package com.wire.android.ui.common

import CheckIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
internal fun WireDropdown(
    items: List<String>,
    defaultItemIndex: Int = -1,
    label: String,
    modifier: Modifier,
    onChange: (selectedIndex: Int, selectedValue: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dropDownWidth by remember { mutableStateOf(Size.Zero) }
    var selectedIndex by remember { mutableStateOf(defaultItemIndex) }
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    val defaultText = stringResource(R.string.wire_dropdown_default_indicator)
    val placeHolderText = stringResource(R.string.wire_dropdown_placeholder)
    val focusManager = LocalFocusManager.current
    val dropDownMenuShape = RoundedCornerShape(
        0.dp,
        0.dp,
        MaterialTheme.wireDimensions.textFieldCornerSize,
        MaterialTheme.wireDimensions.textFieldCornerSize
    )
    val borderColor = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline

    Column(modifier) {
        Label(
            label, false, WireTextFieldState.Default,
            remember { MutableInteractionSource() }, wireTextFieldColors()
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(textBoxShape(isExpanded = expanded))
                .background(
                    color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
                    shape = textBoxShape(isExpanded = expanded)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = textBoxShape(expanded)
                ).clickable {
                    focusManager.clearFocus()
                    expanded = !expanded
                }.onSizeChanged {
                    dropDownWidth = it.toSize()
                }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(Modifier.padding(end = 32.dp)) {
                Text(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .align(Alignment.CenterHorizontally).fillMaxWidth(),
                    text = if (selectedIndex != -1) items[selectedIndex] else placeHolderText,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
                )
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Change",
                tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.CenterEnd)
                    .rotate(arrowRotation),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },

            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = dropDownMenuShape
                )
                .clip(dropDownMenuShape)
                .background(
                    color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
                    shape = dropDownMenuShape
                )
                .width(with(LocalDensity.current) { dropDownWidth.width.toDp() }

                )

        ) {
            items.map {
                val currentIndex = items.indexOf(it)
                DropdownItem(
                    it + if (currentIndex == defaultItemIndex) defaultText else String.EMPTY,
                    currentIndex == selectedIndex,
                    onClick = {
                        selectedIndex = currentIndex
                        expanded = false
                        onChange(selectedIndex, it)
                    })
                if (currentIndex != items.size - 1) Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline
                        )
                )
            }
        }
    }
}

@Composable
private fun textBoxShape(isExpanded: Boolean) = if (isExpanded) RoundedCornerShape(
    MaterialTheme.wireDimensions.textFieldCornerSize,
    MaterialTheme.wireDimensions.textFieldCornerSize,
    0.dp,
    0.dp
) else RoundedCornerShape(
    MaterialTheme.wireDimensions.textFieldCornerSize
)

@Composable
private fun DropdownItem(text: String, isSelected: Boolean, onClick: () -> Unit) = DropdownMenuItem(onClick) {
    Text(
        text,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        style = MaterialTheme.wireTypography.body01,
        color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
    )
    if (isSelected) {
        CheckIcon()
    }
}

@Composable
@Preview
private fun WireDropdownPreviewWithLabel() {
    WireDropdown(
        items =
        ConversationOptions.Protocol.values().map { it.name },
        defaultItemIndex = 0,
        "Protocol",
        modifier = Modifier
    ) { i: Int, s: String -> }
}
