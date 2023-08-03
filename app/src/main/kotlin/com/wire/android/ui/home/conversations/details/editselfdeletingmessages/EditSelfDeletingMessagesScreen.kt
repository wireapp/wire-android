/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.folderWithElements

@RootNavGraph
@Destination(
    navArgsDelegate = EditSelfDeletingMessagesNavArgs::class
)
@Composable
fun EditSelfDeletingMessagesScreen(
    navigator: Navigator,
    editSelfDeletingMessagesViewModel: EditSelfDeletingMessagesViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = navigator::navigateBack,
                title = stringResource(id = R.string.self_deleting_messages_title)
            )
        }, snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }) { internalPadding ->
        with(editSelfDeletingMessagesViewModel) {
            Column(modifier = Modifier.padding(internalPadding)) {
                SelfDeletingMessageOption(
                    switchState = state.isEnabled,
                    isLoading = state.isLoading,
                    onCheckedChange = ::updateSelfDeletingMessageOption
                )
                LazyColumn(
                    modifier = Modifier
                        .background(MaterialTheme.wireColorScheme.background)
                        .scrollable(scrollState, orientation = Orientation.Vertical)
                        .weight(1F)
                        .fillMaxSize()
                ) {
                    if (state.isEnabled) {
                        folderWithElements(
                            header = context.resources.getString(R.string.self_deleting_messages_folder_timer),
                            items = SelfDeletionDuration.customValues()
                                .associateBy { it.name },
                            divider = { WireDivider(color = MaterialTheme.wireColorScheme.outline) }
                        ) { duration ->
                            if (duration == SelfDeletionDuration.None) {
                                Text(
                                    text = stringResource(id = R.string.automatically_delete_message_after),
                                    style = MaterialTheme.wireTypography.label04,
                                    color = MaterialTheme.wireColorScheme.secondaryText,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = MaterialTheme.wireColorScheme.surface)
                                        .padding(all = MaterialTheme.wireDimensions.spacing16x)
                                )
                            } else {
                                SelectableSelfDeletingItem(
                                    duration = duration,
                                    isSelected = state.locallySelected == duration,
                                    onSelfDeletionDurationSelected = ::onSelectDuration
                                )
                            }
                        }
                    }
                }
                WireDivider(color = MaterialTheme.wireColorScheme.outline)
                WireButton(
                    loading = state.isLoading,
                    state = if (state.didDurationChange()) WireButtonState.Default else WireButtonState.Disabled,
                    onClick = { applyNewDuration(navigator::navigateBack) },
                    text = stringResource(id = R.string.label_apply),
                    modifier = Modifier.padding(all = MaterialTheme.wireDimensions.spacing16x)
                )
            }
        }
    }
}

@Composable
fun SelectableSelfDeletingItem(
    duration: SelfDeletionDuration,
    isSelected: Boolean,
    onSelfDeletionDurationSelected: (SelfDeletionDuration) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableBackground(isSelected, onClick = { onSelfDeletionDurationSelected(duration) })
            .background(color = MaterialTheme.wireColorScheme.surface)
            .padding(all = MaterialTheme.wireDimensions.spacing16x)
    ) {
        RadioButton(selected = isSelected, onClick = null)
        HorizontalSpace.x8()
        Text(
            text = duration.longLabel.asString(),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground
        )
    }
}

@Preview
@Composable
fun PreviewEditSelfDeletingMessagesScreen() {
    EditSelfDeletingMessagesScreen(rememberNavigator {})
}
