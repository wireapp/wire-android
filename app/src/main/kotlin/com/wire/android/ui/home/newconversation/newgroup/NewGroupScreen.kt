package com.wire.android.ui.home.newconversation.newgroup

import CheckIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.Label
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.ConversationOptions

@Composable
fun NewGroupScreen(
    newGroupState: NewGroupState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onCreateGroup: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit
) {
    NewGroupScreenContent(
        newGroupState = newGroupState,
        onGroupNameChange = onGroupNameChange,
        onContinuePressed = onCreateGroup,
        onGroupNameErrorAnimated = onGroupNameErrorAnimated,
        onBackPressed = onBackPressed
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun NewGroupScreenContent(
    newGroupState: NewGroupState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(newGroupState) {
        Scaffold(topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                title = stringResource(id = R.string.new_group_title)
            )
        }) { internalPadding ->
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(internalPadding)
            ) {
                val (textField, text, button, protocol) = createRefs()
                val keyboardController = LocalSoftwareKeyboardController.current
                Text(
                    text = stringResource(id = R.string.new_group_description),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing24x
                        )
                        .constrainAs(text) {
                            top.linkTo(parent.top)
                        }
                )
                Box(modifier = Modifier.constrainAs(textField) {
                    top.linkTo(parent.top)
                    bottom.linkTo(protocol.bottom)
                }) {
                    ShakeAnimation { animate ->
                        if (animatedGroupNameError) {
                            animate()
                            onGroupNameErrorAnimated()
                        }
                        WireTextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            placeholderText = stringResource(R.string.group_name),
                            labelText = stringResource(R.string.group_name).uppercase(),
                            state = if (error is NewGroupState.GroupNameError.TextFieldError) when (error) {
                                NewGroupState.GroupNameError.TextFieldError.GroupNameEmptyError ->
                                    WireTextFieldState.Error(stringResource(id = R.string.empty_group_name_error))
                                NewGroupState.GroupNameError.TextFieldError.GroupNameExceedLimitError ->
                                    WireTextFieldState.Error(stringResource(id = R.string.group_name_exceeded_limit_error))
                            } else WireTextFieldState.Default,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        )
                    }

                }

                DropDown(keyboardController, this@with,
                    modifier = Modifier.constrainAs(protocol) {
                        top.linkTo(textField.bottom)
                    })

                WirePrimaryButton(
                    text = stringResource(R.string.label_continue),
                    onClick = onContinuePressed,
                    fillMaxWidth = true,
                    loading = isLoading,
                    trailingIcon = Icons.Filled.ChevronRight.Icon(),
                    state = if (continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                        .constrainAs(button) {
                            bottom.linkTo(parent.bottom)
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DropDown(
    keyboardController: SoftwareKeyboardController?, newGroupState: NewGroupState,modifier:Modifier
) {
    with(newGroupState) {
        Column(
            modifier = modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp, bottom = 0.dp
            )
        ) {
            var expanded by remember { mutableStateOf(false) }
            Label(
                "Protocol", false, WireTextFieldState.Default,
                remember { MutableInteractionSource() }, wireTextFieldColors()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = wireTextFieldColors().backgroundColor(WireTextFieldState.Default).value,
                        shape = if (expanded) RoundedCornerShape(
                            MaterialTheme.wireDimensions.textFieldCornerSize,
                            MaterialTheme.wireDimensions.textFieldCornerSize,
                            0.dp,
                            0.dp
                        ) else RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
                    )
                    .border(
                        width = 1.dp,
                        color = wireTextFieldColors().borderColor(
                            WireTextFieldState.Default,
                            remember { MutableInteractionSource() }).value,
                        shape = if (expanded) RoundedCornerShape(
                            MaterialTheme.wireDimensions.textFieldCornerSize,
                            MaterialTheme.wireDimensions.textFieldCornerSize,
                            0.dp,
                            0.dp
                        ) else RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
                    )
            ) {
                Row(modifier = Modifier.clickable {
                    //todo: fix when the keyboard is shown, the dropdown is pop up above the text field
                    keyboardController?.hide()
                    expanded = true
                }) {
                    Text(
                        text = groupProtocol.name + if (groupProtocol == ConversationOptions.Protocol.PROTEUS) " (Default)" else "",
                        modifier = Modifier
                            .padding(
                                start = 16.dp,
                                top = 16.dp, bottom = 16.dp
                            )
                            .weight(1f)
                            .fillMaxWidth(),
                        style = MaterialTheme.wireTypography.body01,
                        color = wireTextFieldColors().labelColor(WireTextFieldState.Default,
                            remember { MutableInteractionSource() }).value
                    )
                    Image(
                        painter = if (expanded) painterResource(R.drawable.ic_dropup_icon) else painterResource(R.drawable.ic_dropdown_icon),
                        contentDescription = "Protocol", modifier = Modifier
                            .padding(
                                end = 16.dp,
                                top = 16.dp, bottom = 16.dp
                            ),

                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .padding(
                                top = 0.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = wireTextFieldColors().borderColor(
                                    WireTextFieldState.Default,
                                    remember { MutableInteractionSource() }).value,
                                shape = RoundedCornerShape(
                                    0.dp,
                                    0.dp,
                                    MaterialTheme.wireDimensions.textFieldCornerSize,
                                    MaterialTheme.wireDimensions.textFieldCornerSize
                                )
                            )
                            .background(
                                color = Color(0xFFFFFFFF),
                            )

                    ) {
                        dropDownItem(
                            ConversationOptions.Protocol.PROTEUS.name + "(Default)",
                            ConversationOptions.Protocol.PROTEUS == groupProtocol,
                            onClick = {
                                groupProtocol = ConversationOptions.Protocol.PROTEUS
                                expanded = false
                            })
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    wireTextFieldColors().borderColor(
                                        WireTextFieldState.Default,
                                        remember { MutableInteractionSource() }).value
                                )
                        )
                        dropDownItem(ConversationOptions.Protocol.MLS.name, ConversationOptions.Protocol.MLS == groupProtocol, onClick = {
                            groupProtocol = ConversationOptions.Protocol.MLS
                            expanded = false
                        })
                    }
                }
            }
        }
    }

}

@Composable
private fun dropDownItem(label: String, isSelected: Boolean, onClick: () -> Unit) = DropdownMenuItem(onClick) {
    Text(
        text = label,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize)
            ),
        style = MaterialTheme.wireTypography.body01,

        )
    if (isSelected) {
        CheckIcon()
    }
}

@Composable
@Preview
private fun NewGroupScreenPreview() {
    NewGroupScreenContent(
        NewGroupState(),
        {},
        {},
        {},
        {}
    )
}
