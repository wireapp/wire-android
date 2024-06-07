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

package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameMode
import com.wire.android.ui.common.groupname.GroupNameScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@RootNavGraph
@Destination(
    navArgsDelegate = EditConversationNameNavArgs::class
)
@Composable
fun EditConversationNameScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<Boolean>,
    viewModel: EditConversationMetadataViewModel = hiltViewModel(),
) {
    with(viewModel) {
        GroupNameScreen(
            newGroupState = editConversationState,
            newGroupNameTextState = editConversationNameTextState,
            onGroupNameErrorAnimated = ::onGroupNameErrorAnimated,
            onContinuePressed = {
                saveNewGroupName(
                    onFailure = {
                        resultNavigator.setResult(false)
                        resultNavigator.navigateBack()
                    },
                    onSuccess = {
                        resultNavigator.setResult(true)
                        resultNavigator.navigateBack()
                    }
                )
            },
            onBackPressed = navigator::navigateBack
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewNewGroupScreen() = WireTheme {
    GroupNameScreen(GroupMetadataState(mode = GroupNameMode.EDITION), TextFieldState(), {}, {}, {})
}
