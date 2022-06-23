package com.wire.android.ui.common

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    items: List<String>, defaultItemIndex: Int = -1, label: String, modifier: Modifier, onChange: (selectedIndex: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(defaultItemIndex) }
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 0f)
    val placeHolderText = stringResource(R.string.wire_dropdown_placeholder)
    val borderColor = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline
    val shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
    val animationDuration = 500
    val expandAnimationDelay = 400
    val animationTweenDuration = 500


    Column(modifier) {
        Label(
            label, false, WireTextFieldState.Default, remember { MutableInteractionSource() }, wireTextFieldColors()
        )
        Column(modifier = Modifier
            .clip(shape)
            .background(color = MaterialTheme.wireColorScheme.secondaryButtonEnabled, shape = shape)
            .border(width = 1.dp, color = borderColor, shape)
            .clickable {
                expanded = !expanded
            }) {
            Row(
                Modifier.padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x,
                    top = dimensions().spacing12x,
                    bottom = dimensions().spacing12x
                )
            ) {
                Column(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                    Text(
                        text = if (selectedIndex != -1) {
                            items[selectedIndex] + LocalContext.current.defaultTextIndicator(selectedIndex, defaultItemIndex)
                        } else placeHolderText,
                        style = MaterialTheme.wireTypography.body01,
                        color = if (selectedIndex != -1) MaterialTheme.wireColorScheme.onSecondaryButtonEnabled
                        else MaterialTheme.wireColorScheme.secondaryText
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.change),
                    tint = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(top = 4.dp).rotate(arrowRotation)
                )
            }
            if (expanded) Column {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(animationSpec = tween(animationDuration, expandAnimationDelay)) + expandVertically(
                        animationSpec = tween(animationTweenDuration)
                    ),
                    exit = fadeOut(animationSpec = tween(animationDuration)) +
                            shrinkVertically(animationSpec = tween(animationTweenDuration))
                ) {
                    Column {
                        Divider()
                        List(items.size) { index ->
                            DropdownItem(text = items[index] + LocalContext.current.defaultTextIndicator(index, defaultItemIndex),
                                isSelected = index == selectedIndex,
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                    onChange(selectedIndex)
                                })
                            if (index != items.size - 1) Divider()
                        }
                    }
                }
            }
        }

    }
}


private fun Context.defaultTextIndicator(index: Int, defaultIndex: Int): String {
    val defaultText = getString(R.string.wire_dropdown_default_indicator)
    return if (index == defaultIndex) defaultText else String.EMPTY
}

@Composable
private fun DropdownItem(text: String, isSelected: Boolean, onClick: () -> Unit) = DropdownMenuItem(
    onClick, Modifier.background(
        color = if (isSelected) MaterialTheme.wireColorScheme.secondaryButtonSelected
        else MaterialTheme.wireColorScheme.tertiaryButtonEnabled
    )
) {
    Text(
        text,
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
@Preview
private fun WireDropdownPreviewWithLabel() {
    WireDropDown(
        items = ConversationOptions.Protocol.values().map { it.name }, defaultItemIndex = 0, "Protocol", modifier = Modifier
    ) {}
}
