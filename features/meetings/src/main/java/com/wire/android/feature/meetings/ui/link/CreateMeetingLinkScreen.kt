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
package com.wire.android.feature.meetings.ui.link

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.create.MeetingPasswordInput
import com.wire.android.feature.meetings.ui.createMeetingLinkViewModel
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.android.ui.common.R as CommonR

@Composable
internal fun CreateMeetingLinkScreen(
    meetingId: MeetingId,
    onBack: () -> Unit,
    viewModel: CreateMeetingLinkViewModel = createMeetingLinkViewModel(meetingId),
) {
    HandleActions(viewModel.actions) { action ->
        when (action) {
            CreateMeetingLinkViewAction.Success -> onBack()
        }
    }
    when (val state = viewModel.state) {
        CreateMeetingLinkState.Loading -> Unit
        CreateMeetingLinkState.NotAvailable -> LaunchedEffect(state) { onBack() }
        is CreateMeetingLinkState.Ready -> CreateMeetingLinkContent(state, viewModel, onBack)
    }
}

@Composable
private fun CreateMeetingLinkContent(
    state: CreateMeetingLinkState.Ready,
    viewModel: CreateMeetingLinkViewModel,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val dismiss = { if (!state.isSubmitting) onBack() }
    BackHandler(onBack = dismiss)
    WireScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.meeting_link_create_title),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = dismiss,
                elevation = scrollState.rememberTopBarElevationState().value,
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(dimensions().spacing16x),
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
                ) {
                    WireSecondaryButton(
                        text = stringResource(CommonR.string.label_cancel),
                        onClick = dismiss,
                        state = if (state.isSubmitting) WireButtonState.Disabled else WireButtonState.Default,
                        modifier = Modifier.weight(1f),
                    )
                    WirePrimaryButton(
                        text = stringResource(if (state.isSubmitting) R.string.meeting_link_creating else R.string.meeting_link_create),
                        onClick = { viewModel.onCreate() },
                        loading = state.isSubmitting,
                        state = if (state.isSubmitting) WireButtonState.Disabled else WireButtonState.Default,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(CommonR.drawable.ic_refresh),
                                contentDescription = null,
                                modifier = Modifier.padding(end = dimensions().spacing4x),
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing24x),
        ) {
            Text(stringResource(R.string.meeting_link_create_description), style = typography().body01)
            MeetingPasswordInput(
                passwordState = viewModel.passwordTextState,
                confirmPasswordState = viewModel.confirmPasswordTextState,
                invalidPassword = state.invalidPassword,
                passwordsDoNotMatch = state.passwordsDoNotMatch,
                onGenerateRandomPassword = viewModel::onGenerateRandomPassword,
                readOnly = state.isSubmitting,
            )
        }
    }
    if (state.submitError) {
        WireDialog(
            title = stringResource(R.string.meeting_link_refresh_failed_title),
            text = stringResource(R.string.meeting_link_refresh_failed),
            onDismiss = viewModel::onDismissError,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = viewModel::onDismissError,
                text = stringResource(CommonR.string.label_ok),
                type = WireDialogButtonType.Primary,
            ),
        )
    }
    if (state.confirmWithoutPassword) {
        WireDialog(
            title = stringResource(R.string.meeting_link_create_title),
            text = stringResource(R.string.meeting_link_remove_password_description),
            onDismiss = viewModel::onDismissConfirmation,
            buttonsHorizontalAlignment = false,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { viewModel.onCreate(confirmWithoutPassword = true) },
                text = stringResource(R.string.meeting_link_create_without_password),
                type = WireDialogButtonType.Primary,
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = viewModel::onDismissConfirmation,
                text = stringResource(CommonR.string.label_cancel),
            ),
        )
    }
}
