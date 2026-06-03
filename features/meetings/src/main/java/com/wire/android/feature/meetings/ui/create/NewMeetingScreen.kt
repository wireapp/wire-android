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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.meetings.WireNewMeetingDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@WireNewMeetingDestination(
    start = true,
    navArgs = NewMeetingNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun NewMeetingScreen(
    navigator: WireNavigator,
    navArgs: NewMeetingNavArgs,
) {
    val viewModel =
    NewMeetingContent(
        type = navArgs.type,
        onBackPressed = navigator::navigateBack,
        titleState = rememberTextFieldState(), // TODO
    )
}

@Composable
fun NewMeetingContent(
    titleState: TextFieldState,
    type: NewMeetingType,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(type.title),
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(
                    contentDescription = R.string.contnt_description_new_meeting_back_icon
                ),
            )
        },
        content = { internalPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(internalPadding)
                    .padding(
                        top = dimensions().spacing24x,
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                    )
            ) {
                TitleInput(titleState = titleState)
            }
        },
        bottomBar = {
            Surface(
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                    WirePrimaryButton(
                        text = stringResource(type.action),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(type.icon),
                                contentDescription = null, // no separate content description as the text already describes the action
                                modifier = Modifier.padding(dimensions().spacing4x),
                            )
                        },
                        state = WireButtonState.Disabled, // TODO
                        onClick = { /*TODO*/ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensions().spacing16x),
                    )
            }
        }
    )
}

@Composable
private fun TitleInput(
    titleState: TextFieldState,
) {
    WireTextField(
        textState = titleState,
        placeholderText = stringResource(R.string.new_meeting_title_input_placeholder),
        labelText = stringResource(R.string.new_meeting_title_input_label).uppercase(),
        semanticDescription = stringResource(R.string.new_meeting_title_input_placeholder),
        keyboardOptions = KeyboardOptions.DefaultEmailDone,
        testTag = "titleInput",
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewNewMeetingScreen_MeetNow() = WireTheme {
    NewMeetingContent(
        titleState = rememberTextFieldState(),
        type = NewMeetingType.MeetNow,
        onBackPressed = {},
    )
}
