package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.ShakeAnimation
import com.wire.android.ui.common.WireDropdown
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
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

                WireDropdown(
                    items =
                    ConversationOptions.Protocol.values().map { it.name },
                    defaultItemIndex = 0,
                    stringResource(R.string.protocol),
                    modifier = Modifier.constrainAs(protocol) {
                        top.linkTo(textField.bottom)
                    }.padding(MaterialTheme.wireDimensions.spacing16x)
                ) { selectedIndex, _ ->
                    groupProtocol = ConversationOptions.Protocol.values()[selectedIndex]
                }
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
