/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.updatechannelaccess

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessScreenContent

@WireRootDestination(
    navArgs = UpdateChannelAccessArgs::class,
    style = SlideNavigationAnimation::class,
)
@Composable
fun ChannelAccessOnUpdateScreen(
    resultNavigator: ResultBackNavigator<UpdateChannelAccessArgs>,
    updateChannelAccessViewModel: UpdateChannelAccessViewModel = hiltViewModel()
) {

    fun navigateBack() {
        resultNavigator.setResult(
            UpdateChannelAccessArgs(
                updateChannelAccessViewModel.conversationId,
                updateChannelAccessViewModel.accessType,
                updateChannelAccessViewModel.permissionType,
            )
        )
        resultNavigator.navigateBack()
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { navigateBack() },
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.channel_access_label),
                titleContentDescription = stringResource(id = R.string.content_description_new_conversation_options_heading),
                navigationIconType = NavigationIconType.Back(R.string.content_description_user_profile_close_btn)
            )
        }
    ) { internalPadding ->
        with(updateChannelAccessViewModel) {
            ChannelAccessScreenContent(
                internalPadding = internalPadding,
                accessType,
                permissionType,
                onAccessChange = ::updateChannelAccess,
                onPermissionChange = ::updateChannelAddPermission
            )
        }
    }

    BackHandler {
        navigateBack()
    }
}
