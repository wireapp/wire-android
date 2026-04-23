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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireTertiaryButton
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
import com.wire.android.util.ui.PreviewMultipleThemes

@WireRootDestination(style = SlideNavigationAnimation::class)
@Composable
fun AiCustomPromptScreen(
    resultNavigator: ResultBackNavigator<String>,
) {
    val textState = remember { TextFieldState() }
    AiCustomPromptContent(
        textState = textState,
        onApply = { resultNavigator.navigateBack(textState.text.toString()) },
        onBack = { resultNavigator.navigateBack() },
        onSelectPredefinedPrompt = { resultNavigator.navigateBack(it) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiCustomPromptContent(
    textState: TextFieldState,
    onApply: () -> Unit,
    onBack: () -> Unit,
    onSelectPredefinedPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isApplyEnabled by remember { derivedStateOf { textState.text.isNotBlank() } }
    val predefinedPrompts = listOf(
        stringResource(R.string.ai_predefined_prompt_shorter),
        stringResource(R.string.ai_predefined_prompt_longer),
        stringResource(R.string.ai_predefined_prompt_fix_grammar),
        stringResource(R.string.ai_predefined_prompt_professional),
        stringResource(R.string.ai_predefined_prompt_simplify),
        stringResource(R.string.ai_predefined_prompt_casual),
    )

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = onBack,
                navigationIconType = NavigationIconType.Back(),
                title = stringResource(R.string.title_ai_custom_prompt),
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .fillMaxWidth()
            ) {
                WireTextField(
                    textState = textState,
                    placeholderText = stringResource(R.string.hint_ai_custom_prompt),
                    state = WireTextFieldState.Default,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                )
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.spacing8x),
                horizontalArrangement = Arrangement.Start,
            ) {
                predefinedPrompts.forEach { prompt ->
                    WireTertiaryButton(
                        text = prompt,
                        onClick = { onSelectPredefinedPrompt(prompt) },
                        fillMaxWidth = false,
                        borderWidth = 0.dp,
                    )
                }
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background,
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    WirePrimaryButton(
                        text = stringResource(R.string.label_apply),
                        onClick = onApply,
                        fillMaxWidth = true,
                        state = if (isApplyEnabled) WireButtonState.Default else WireButtonState.Disabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAiCustomPromptContent() = WireTheme {
    AiCustomPromptContent(
        textState = TextFieldState("Make this shorter"),
        onApply = {},
        onBack = {},
        onSelectPredefinedPrompt = {},
    )
}
