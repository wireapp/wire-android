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

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.groupname.GroupNameMode.CREATION
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun GroupNameScreen(
    newGroupState: GroupMetadataState,
    newGroupNameTextState: TextFieldState,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    with(newGroupState) {
        val scrollState = rememberScrollState()
        WireScaffold(
            modifier = modifier,
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = scrollState.rememberTopBarElevationState().value,
                    onNavigationPressed = onBackPressed,
                    title = stringResource(id = getScreenName()),
                    navigationIconType = NavigationIconType.Back(R.string.content_description_new_conversation_name_back_btn)
                )
            }
        ) { internalPadding ->

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
                    val description = if (newGroupState.isChannel) {
                        R.string.channel_name_description
                    } else {
                        R.string.group_name_description
                    }
                    Text(
                        text = stringResource(id = description),
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
                            val labelText = if (newGroupState.isChannel) {
                                R.string.channel_name_title
                            } else {
                                R.string.group_name_title
                            }
                            val semanticDescriptionTextField = if (newGroupState.isChannel) {
                                R.string.content_description_new_channel_name_field
                            } else {
                                R.string.content_description_new_group_name_field
                            }
                            WireTextField(
                                textState = newGroupNameTextState,
                                placeholderText = stringResource(R.string.conversation_name_placeholder),
                                labelText = stringResource(labelText).uppercase(),
                                state = computeGroupMetadataState(newGroupState.isChannel, error),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                onKeyboardAction = { keyboardController?.hide() },
                                semanticDescription = stringResource(id = semanticDescriptionTextField),
                                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .width(dimensions().spacing64x)
                                            .height(dimensions().spacing40x),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = newGroupNameTextState.text.isNotBlank(),
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            if (isLoading) {
                                                WireCircularProgressIndicator(
                                                    modifier = Modifier.padding(
                                                        top = dimensions().spacing12x,
                                                        bottom = dimensions().spacing12x,
                                                        end = dimensions().spacing32x
                                                    ),
                                                    progressColor = MaterialTheme.wireColorScheme.onSurface
                                                )
                                            }
                                            IconButton(
                                                modifier = Modifier.padding(start = dimensions().spacing12x),
                                                onClick = newGroupNameTextState::clearText,
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_clear_search),
                                                    contentDescription = stringResource(R.string.content_description_clear_content)
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (mode == CREATION && !newGroupState.isChannel) {
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
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(commonR.drawable.ic_chevron_right),
                                    contentDescription = null,
                                )
                            },
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

fun GroupMetadataState.getScreenName(): Int = when {
    isChannel && mode == CREATION -> R.string.new_channel_title
    isChannel -> R.string.channel_name_title
    mode == CREATION -> R.string.new_group_title
    else -> R.string.group_name_title
}

@Composable
private fun computeGroupMetadataState(isChannelsAllowed: Boolean, error: GroupMetadataState.NewGroupError) =
    if (error is GroupMetadataState.NewGroupError.TextFieldError) {
        when (error) {
            GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError -> {
                val errorMessage = if (isChannelsAllowed) {
                    R.string.empty_channel_name_error
                } else {
                    R.string.empty_regular_group_name_error
                }
                WireTextFieldState.Error(stringResource(id = errorMessage))
            }

            GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError -> {
                val errorMessage = if (isChannelsAllowed) {
                    R.string.channel_name_exceeded_limit_error
                } else {
                    R.string.regular_group_name_exceeded_limit_error
                }
                WireTextFieldState.Error(stringResource(id = errorMessage))
            }
        }
    } else {
        WireTextFieldState.Default
    }

@PreviewMultipleThemes
@Composable
fun PreviewGroupNameScreenEdit() = WireTheme {
    GroupNameScreen(
        newGroupState = GroupMetadataState(),
        newGroupNameTextState = TextFieldState("group name"),
        onContinuePressed = {},
        onGroupNameErrorAnimated = {},
        onBackPressed = {}
    )
}
