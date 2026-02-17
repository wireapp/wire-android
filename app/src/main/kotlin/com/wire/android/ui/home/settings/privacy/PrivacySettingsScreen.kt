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

package com.wire.android.ui.home.settings.privacy

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme

@WireRootDestination
@Composable
fun PrivacySettingsConfigScreen(
    navigator: Navigator,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    with(viewModel) {
        PrivacySettingsScreenContent(
            isAnonymousUsageDataEnabled = state.isAnalyticsUsageEnabled,
            shouldShowAnalyticsUsage = state.shouldShowAnalyticsUsage,
            areReadReceiptsEnabled = state.areReadReceiptsEnabled,
            setReadReceiptsState = ::setReadReceiptsState,
            isTypingIndicatorEnabled = state.isTypingIndicatorEnabled,
            setTypingIndicatorState = ::setTypingIndicatorState,
            screenshotCensoringConfig = state.screenshotCensoringConfig,
            setScreenshotCensoringConfig = ::setScreenshotCensoringConfig,
            setAnonymousUsageDataEnabled = ::setAnonymousUsageDataEnabled,
            onBackPressed = navigator::navigateBack,
        )
    }
}

@Composable
fun PrivacySettingsScreenContent(
    isAnonymousUsageDataEnabled: Boolean,
    shouldShowAnalyticsUsage: Boolean,
    areReadReceiptsEnabled: Boolean,
    setReadReceiptsState: (Boolean) -> Unit,
    isTypingIndicatorEnabled: Boolean,
    setTypingIndicatorState: (Boolean) -> Unit,
    screenshotCensoringConfig: ScreenshotCensoringConfig,
    setScreenshotCensoringConfig: (Boolean) -> Unit,
    setAnonymousUsageDataEnabled: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.settings_privacy_settings_label)
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            if (shouldShowAnalyticsUsage) {
                GroupConversationOptionsItem(
                    title = stringResource(id = R.string.settings_send_anonymous_usage_data_title),
                    switchState = SwitchState.Enabled(value = isAnonymousUsageDataEnabled, onCheckedChange = setAnonymousUsageDataEnabled),
                    arrowType = ArrowType.NONE,
                    subtitle = stringResource(id = R.string.settings_send_anonymous_usage_data_description)
                )
            }
            WireDivider(color = colorsScheme().divider)
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_send_read_receipts),
                switchState = SwitchState.Enabled(value = areReadReceiptsEnabled, onCheckedChange = setReadReceiptsState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )
            WireDivider(color = colorsScheme().divider)
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
            WireDivider(color = colorsScheme().divider)
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_show_typing_indicator_title),
                switchState = SwitchState.Enabled(value = isTypingIndicatorEnabled, onCheckedChange = setTypingIndicatorState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_show_typing_indicator_description)
            )
        }
    }
}

@Composable
@MultipleThemePreviews
fun PreviewSendReadReceipts() = WireTheme {
    PrivacySettingsScreenContent(
        isAnonymousUsageDataEnabled = true,
        shouldShowAnalyticsUsage = true,
        areReadReceiptsEnabled = true,
        setReadReceiptsState = {},
        isTypingIndicatorEnabled = true,
        setTypingIndicatorState = {},
        screenshotCensoringConfig = ScreenshotCensoringConfig.DISABLED,
        setScreenshotCensoringConfig = {},
        setAnonymousUsageDataEnabled = {},
        onBackPressed = {},
    )
}
