/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.poll

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultText
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.createPollViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler

@WireRootDestination(
    navArgs = CreatePollNavArgs::class,
    style = SlideNavigationAnimation::class,
)
@Composable
fun CreatePollScreen(
    navigator: Navigator,
    viewModel: CreatePollViewModel = createPollViewModel()
) {
    HandleActions(viewModel.actions) { action ->
        when (action) {
            CreatePollAction.NavigateBack -> navigator.navigateBack()
        }
    }

    CreatePollScreenContent(
        questionTextState = viewModel.questionTextState,
        optionTextStates = viewModel.optionTextStates,
        state = viewModel.state,
        onBackPressed = navigator::navigateBack,
        onAddOptionClicked = viewModel::addOption,
        onRemoveOptionClicked = viewModel::removeOption,
        onAllowMultipleAnswersChanged = viewModel::setAllowMultipleAnswers,
        onHideVoterNamesChanged = viewModel::setHideVoterNames,
        onSendClicked = viewModel::sendPoll,
    )

    SnackBarMessageHandler(viewModel.infoMessage)
}

@Composable
private fun CreatePollScreenContent(
    questionTextState: TextFieldState,
    optionTextStates: List<TextFieldState>,
    state: CreatePollState,
    onBackPressed: () -> Unit,
    onAddOptionClicked: () -> Unit,
    onRemoveOptionClicked: (Int) -> Unit,
    onAllowMultipleAnswersChanged: (Boolean) -> Unit,
    onHideVoterNamesChanged: (Boolean) -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(),
                title = stringResource(id = R.string.create_poll_title)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background
            ) {
                WirePrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.wireDimensions.spacing16x),
                    text = stringResource(id = R.string.create_poll_send),
                    state = if (state.canSend) WireButtonState.Default else WireButtonState.Disabled,
                    loading = state.isSending,
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.padding(end = MaterialTheme.wireDimensions.spacing8x),
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = null,
                            tint = colorsScheme().onPrimaryButtonEnabled
                        )
                    },
                    onClick = onSendClicked,
                )
            }
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(MaterialTheme.wireDimensions.spacing16x)
        ) {
            SectionLabel(text = stringResource(id = R.string.create_poll_question_label))
            VerticalSpace.x8()
            WireTextField(
                textState = questionTextState,
                placeholderText = stringResource(id = R.string.create_poll_question_placeholder),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions.DefaultText,
                modifier = Modifier.fillMaxWidth(),
            )

            VerticalSpace.x24()
            SectionLabel(text = stringResource(id = R.string.create_poll_options_label))
            VerticalSpace.x8()
            optionTextStates.forEachIndexed { index, optionTextState ->
                PollOptionField(
                    textState = optionTextState,
                    optionNumber = index + 1,
                    canRemove = optionTextStates.size > CreatePollViewModel.MIN_OPTIONS,
                    onRemoveClicked = { onRemoveOptionClicked(index) }
                )
                VerticalSpace.x8()
            }
            WireSecondaryButton(
                text = stringResource(id = R.string.create_poll_add_option),
                state = if (state.canAddOption) WireButtonState.Default else WireButtonState.Disabled,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.padding(end = MaterialTheme.wireDimensions.spacing8x),
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                    )
                },
                onClick = onAddOptionClicked,
            )

            VerticalSpace.x24()
            SectionLabel(text = stringResource(id = R.string.create_poll_settings_label))
            VerticalSpace.x8()
            PollSettingRow(
                text = stringResource(id = R.string.create_poll_allow_multiple_answers),
                checked = state.allowMultipleAnswers,
                onCheckedChange = onAllowMultipleAnswersChanged
            )
            HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
            PollSettingRow(
                text = stringResource(id = R.string.create_poll_hide_voter_names),
                checked = state.hideVoterNames,
                onCheckedChange = onHideVoterNamesChanged
            )
        }
    }
}

@Composable
private fun PollOptionField(
    textState: TextFieldState,
    optionNumber: Int,
    canRemove: Boolean,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireTextField(
        textState = textState,
        placeholderText = stringResource(id = R.string.create_poll_option_placeholder, optionNumber),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions.DefaultText,
        trailingIcon = if (canRemove) {
            {
                IconButton(onClick = onRemoveClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.create_poll_remove_option_content_description),
                        tint = MaterialTheme.wireColorScheme.secondaryText
                    )
                }
            }
        } else {
            null
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PollSettingRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = MaterialTheme.wireDimensions.spacing12x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        WireSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.wireTypography.label01,
        color = MaterialTheme.wireColorScheme.secondaryText,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCreatePollScreenContent() = WireTheme {
    CreatePollScreenContent(
        questionTextState = TextFieldState("Lunch?"),
        optionTextStates = listOf(TextFieldState("Pizza"), TextFieldState("Sushi")),
        state = CreatePollState(canSend = true),
        onBackPressed = {},
        onAddOptionClicked = {},
        onRemoveOptionClicked = {},
        onAllowMultipleAnswersChanged = {},
        onHideVoterNamesChanged = {},
        onSendClicked = {},
    )
}
