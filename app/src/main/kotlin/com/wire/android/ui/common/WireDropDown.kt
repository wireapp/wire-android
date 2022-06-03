package com.wire.android.ui.common

import CheckIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import com.wire.android.R
import com.wire.android.ui.common.textfield.Label
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.ConversationOptions
import okhttp3.internal.wait

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WireDropdown(
    items: List<String>,
    defaultItemIndex: Int = -1,
    label: String,
    modifier: Modifier,
    onSelected: (selectedIndex: Int, selectedValue: String) -> Unit
) {
    val menuShape = RoundedCornerShape(
        0.dp,
        0.dp,
        MaterialTheme.wireDimensions.textFieldCornerSize,
        MaterialTheme.wireDimensions.textFieldCornerSize
    )

    val defaultText = " (Default)"
    val preSelectionText = "Select an Item"
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }
    var selectedIndex by remember { mutableStateOf(defaultItemIndex) }
    val rotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    val indicatorWidth = (if (expanded) 2 else 1).dp
    val indicatorColor =
        if (expanded) Color.Yellow.copy(alpha = ContentAlpha.high)
        else Color.Yellow.copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
//
//        Column(
//            modifier = modifier
//                .padding(
//                    start = MaterialTheme.wireDimensions.textFieldCornerSize,
//                    end = MaterialTheme.wireDimensions.textFieldCornerSize,
//                    top = MaterialTheme.wireDimensions.textFieldCornerSize, bottom = 0.dp
//                )
//                .background(Color.Transparent)
//        ) {
//
//            Label(
//                dropDownLabel, false, WireTextFieldState.Default,
//                remember { MutableInteractionSource() }, wireTextFieldColors()
//            )
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .onGloballyPositioned { coordinates ->
//                        //This value is used to assign to the DropDown the same width
//                        textfieldSize = coordinates.size.toSize()
//                    }
//                    .background(
//                        color = Color.White,
//                        shape = if (expanded) RoundedCornerShape(
//                            MaterialTheme.wireDimensions.textFieldCornerSize,
//                            MaterialTheme.wireDimensions.textFieldCornerSize,
//                            0.dp,
//                            0.dp
//                        ) else RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
//                    )
//                    .border(
//                        width = 1.dp,
//                        color = wireTextFieldColors().borderColor(
//                            WireTextFieldState.Default,
//                            remember { MutableInteractionSource() }).value,
//                        shape = if (expanded) RoundedCornerShape(
//                            MaterialTheme.wireDimensions.textFieldCornerSize,
//                            MaterialTheme.wireDimensions.textFieldCornerSize,
//                            0.dp,
//                            0.dp
//                        ) else RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
//                    )
//            ) {
//
//                Box(
//                    Modifier
//                        .fillMaxWidth()
//                        .background(
//                            color = Color.White,
//                            shape = textBoxShape(isExpanded = expanded)
//                        )
////
//                        .onGloballyPositioned { textfieldSize = it.size.toSize() }
//                        .clip(textBoxShape(isExpanded = expanded))
//                        .clickable {
//                            expanded = !expanded
//                            focusManager.clearFocus()
//                        }
//                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
//                ) {
//                    Column(Modifier.padding(end = 32.dp)) {
//                        Text(
//                            modifier = Modifier
//                                .padding(top = 1.dp)
//                                .align(Alignment.CenterHorizontally),
//                            text = groupProtocol.name + defaultIndicatorText,
//                            style = MaterialTheme.wireTypography.body01,
//                            color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
//                        )
//                    }
//                    Icon(
//                        imageVector = Icons.Filled.ExpandMore,
//                        contentDescription = "Change",
//                        tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
//                        modifier = Modifier
//                            .padding(top = 4.dp)
//                            .align(Alignment.CenterEnd)
//                            .rotate(rotation),
//                    )
//
//
//
//                }
//            }
//        }
//        DropdownMenu(
//            expanded = expanded,
//            properties = PopupProperties(focusable = true),
//            onDismissRequest = { expanded = false },
//            modifier = Modifier
//                .border(
//                    width = 1.dp,
//                    color = wireTextFieldColors().borderColor(
//                        WireTextFieldState.Default,
//                        remember { MutableInteractionSource() }).value,
//                    shape = menuShape
//                )
//                .clip(menuShape)
//                .background(
//                            color = Color.Red,
//                            shape = menuShape
//                        )
//        ) {
//            DropdownItem(
//                ConversationOptions.Protocol.PROTEUS.name + defaultText,
//                ConversationOptions.Protocol.PROTEUS == groupProtocol,
//                onClick = {
//                    groupProtocol = ConversationOptions.Protocol.PROTEUS
//                    expanded = false
//                })
//            Spacer(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(1.dp)
//                    .background(
//                        wireTextFieldColors().borderColor(
//                            WireTextFieldState.Default,
//                            remember { MutableInteractionSource() }).value
//                    )
//            )
//            DropdownItem(ConversationOptions.Protocol.MLS.name,
//                ConversationOptions.Protocol.MLS == groupProtocol,
//                onClick = {
//                    groupProtocol = ConversationOptions.Protocol.MLS
//                    expanded = false
//                })
//        }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = modifier
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                top = MaterialTheme.wireDimensions.spacing16x,
                bottom = 0.dp
            )
    ) {
        Column {
            Label(
                label, false, WireTextFieldState.Default,
                remember { MutableInteractionSource() }, wireTextFieldColors()
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(textBoxShape(isExpanded = expanded))
                    .background(
                        color = Color.White,
                        shape = textBoxShape(isExpanded = expanded)
                    )
                    .onGloballyPositioned { textFieldSize = it.size.toSize() }
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = textBoxShape(expanded)
                    )
//                    .clickable {
//                        expanded = !expanded
////                        focusManager.clearFocus()
//                    }
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Column(Modifier.padding(end = 32.dp)) {
                    Text(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .align(Alignment.CenterHorizontally),
                        text = if (selectedIndex != -1) items[selectedIndex] else preSelectionText,
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
                        .rotate(rotation),
                )
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .clip(menuShape)
                .background(
                    color = Color.White,
                    shape = menuShape
                )
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = menuShape
                )
        ) {
            items.map {
                val currentIndex = items.indexOf(it)
                DropdownItem(
                    it + if (currentIndex == defaultItemIndex) defaultText else "",
                    currentIndex == selectedIndex,
                    onClick = {
                        selectedIndex = currentIndex
                        expanded = false
                        onSelected(selectedIndex, it)
                    })
                if (currentIndex != items.size - 1) Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            wireTextFieldColors().borderColor(
                                WireTextFieldState.Default,
                                remember { MutableInteractionSource() }).value
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
