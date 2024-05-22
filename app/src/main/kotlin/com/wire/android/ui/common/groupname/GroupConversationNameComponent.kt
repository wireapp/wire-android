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

package com.wire.android.ui.common.groupname

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.groupname.GroupNameMode.CREATION
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GroupNameScreen(
    newGroupState: GroupMetadataState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(newGroupState) {
        val scrollState = rememberScrollState()

        WireScaffold(topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = onBackPressed,
                title = stringResource(id = if (mode == CREATION) R.string.new_group_title else R.string.group_name_title)
            )
        }) { internalPadding ->

            Column(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    Text(
                        text = stringResource(id = R.string.group_name_description),
                        style = MaterialTheme.wireTypography.body01,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.wireDimensions.spacing16x,
                                vertical = MaterialTheme.wireDimensions.spacing24x
                            )
                    )
                    Box {
                        ShakeAnimation { animate ->
                            if (animatedGroupNameError) {
                                animate()
                                onGroupNameErrorAnimated()
                            }
                            WireTextField(
                                value = groupName,
                                onValueChange = onGroupNameChange,
                                placeholderText = stringResource(R.string.group_name_placeholder),
                                labelText = stringResource(R.string.group_name_title).uppercase(),
                                state = computeGroupMetadataState(error),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                onKeyboardAction = { keyboardController?.hide() },
                                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                            )
                        }
                    }
                    if (mode == CREATION) {
                        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing16x))
                        Text(
                            text = stringResource(R.string.protocol),
                            style = MaterialTheme.wireTypography.label01,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                                .padding(bottom = MaterialTheme.wireDimensions.spacing4x)
                        )
                        Text(
                            text = groupProtocol.name,
                            style = MaterialTheme.wireTypography.body02,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }

                Surface(
                    shadowElevation = scrollState.rememberBottomBarElevationState().value,
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                        WirePrimaryButton(
                            text = stringResource(if (mode == CREATION) R.string.label_continue else R.string.label_ok),
                            onClick = onContinuePressed,
                            fillMaxWidth = true,
                            loading = isLoading,
                            trailingIcon = Icons.Filled.ChevronRight.Icon(),
                            state = if (continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun computeGroupMetadataState(error: GroupMetadataState.NewGroupError) =
    if (error is GroupMetadataState.NewGroupError.TextFieldError) when (error) {
        GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError ->
            WireTextFieldState.Error(stringResource(id = R.string.empty_group_name_error))

        GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError ->
            WireTextFieldState.Error(stringResource(id = R.string.group_name_exceeded_limit_error))
    } else {
        WireTextFieldState.Default
    }

@Preview
@Composable
fun PreviewGroupNameScreenEdit() {
    GroupNameScreen(
        GroupMetadataState(groupName = TextFieldValue("group name")),
        onGroupNameChange = {},
        onContinuePressed = {},
        onGroupNameErrorAnimated = {},
        onBackPressed = {}
    )
}
