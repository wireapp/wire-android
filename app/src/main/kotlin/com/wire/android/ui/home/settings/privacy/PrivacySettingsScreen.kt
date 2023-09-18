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

package com.wire.android.ui.home.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState

@RootNavGraph
@Destination
@Composable
fun PrivacySettingsConfigScreen(
    navigator: Navigator,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    with(viewModel) {
        PrivacySettingsScreenContent(
            areReadReceiptsEnabled = state.areReadReceiptsEnabled,
            setReadReceiptsState = ::setReadReceiptsState,
            isTypingIndicatorEnabled = state.isTypingIndicatorEnabled,
            setTypingIndicatorState = ::setTypingIndicatorState,
            screenshotCensoringConfig = state.screenshotCensoringConfig,
            setScreenshotCensoringConfig = ::setScreenshotCensoringConfig,
            onBackPressed = navigator::navigateBack
        )
    }
}

@Composable
fun PrivacySettingsScreenContent(
    areReadReceiptsEnabled: Boolean,
    setReadReceiptsState: (Boolean) -> Unit,
    isTypingIndicatorEnabled: Boolean,
    setTypingIndicatorState: (Boolean) -> Unit,
    screenshotCensoringConfig: ScreenshotCensoringConfig,
    setScreenshotCensoringConfig: (Boolean) -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onBackPressed,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_privacy_settings_label)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_send_read_receipts),
                switchState = SwitchState.Enabled(value = areReadReceiptsEnabled, onCheckedChange = setReadReceiptsState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_censor_screenshots),
                switchState = when (screenshotCensoringConfig) {
                    ScreenshotCensoringConfig.DISABLED ->
                        SwitchState.Enabled(value = false, onCheckedChange = setScreenshotCensoringConfig)

                    ScreenshotCensoringConfig.ENABLED_BY_USER ->
                        SwitchState.Enabled(value = true, onCheckedChange = setScreenshotCensoringConfig)

                    ScreenshotCensoringConfig.ENFORCED_BY_TEAM ->
                        SwitchState.Disabled(value = true)
                },
                arrowType = ArrowType.NONE,
                subtitle = stringResource(
                    id = when (screenshotCensoringConfig) {
                        ScreenshotCensoringConfig.ENFORCED_BY_TEAM -> R.string.settings_censor_screenshots_enforced_by_team_description
                        else -> R.string.settings_censor_screenshots_description
                    }
                )
            )
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_show_typing_indicator_title),
                switchState = SwitchState.Enabled(value = isTypingIndicatorEnabled, onCheckedChange = setTypingIndicatorState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )
        }
    }
}

@Composable
@Preview
fun PreviewSendReadReceipts() {
    PrivacySettingsScreenContent(true, {}, true, {}, ScreenshotCensoringConfig.DISABLED, {}, {})
}
