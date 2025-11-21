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
package com.wire.android.ui.home.newconversation.channelaccess
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@NewConversationNavGraph
@Destination<WireRootNavGraph>
@Composable
fun ChannelAccessOnCreateScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = navigator::navigateBack,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.channel_access_label),
                titleContentDescription = stringResource(id = R.string.content_description_new_conversation_options_heading),
                navigationIconType = NavigationIconType.Back(R.string.content_description_new_group_options_back_btn)
            )
        }
    ) { internalPadding ->
        ChannelAccessScreenContent(
            internalPadding = internalPadding,
            newConversationViewModel.newGroupState.channelAccessType,
            newConversationViewModel.newGroupState.channelAddPermissionType,
            onAccessChange = newConversationViewModel::setChannelAccess,
            onPermissionChange = newConversationViewModel::setChannelPermission
        )
    }
}

@Composable
fun ChannelAccessScreenContent(
    internalPadding: PaddingValues,
    selectedAccess: ChannelAccessType,
    selectedPermission: ChannelAddPermissionType,
    modifier: Modifier = Modifier,
    onAccessChange: (ChannelAccessType) -> Unit = {},
    onPermissionChange: (ChannelAddPermissionType) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = internalPadding
    ) {
        item {
            AccessSection(
                selectedAccess = selectedAccess,
                onAccessChange = onAccessChange
            )
        }
        if (selectedAccess == ChannelAccessType.PRIVATE) {
            item {
                PermissionSection(
                    selectedPermission = selectedPermission,
                    onPermissionChange = onPermissionChange
                )
            }
        }
    }
}

@Composable
private fun AccessSection(
    selectedAccess: ChannelAccessType,
    modifier: Modifier = Modifier,
    onAccessChange: (ChannelAccessType) -> Unit,
) {
    Column(modifier = modifier) {
        AccessScreenItem(
            channelAccessType = ChannelAccessType.PUBLIC,
            selectedAccess = selectedAccess,
            onItemClicked = onAccessChange,
            isEnabled = false
        )
        AccessScreenItem(
            channelAccessType = ChannelAccessType.PRIVATE,
            selectedAccess = selectedAccess,
            onItemClicked = onAccessChange
        )
        Text(
            text = stringResource(id = R.string.channel_access_description),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier.padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x)
        )
    }
}

@Composable
private fun PermissionSection(
    selectedPermission: ChannelAddPermissionType,
    modifier: Modifier = Modifier,
    onPermissionChange: (ChannelAddPermissionType) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.label_add_participants).uppercase(),
            modifier = Modifier.padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                top = dimensions().spacing28x,
                bottom = dimensions().spacing8x
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        PermissionItem(
            channelAddPermissionType = ChannelAddPermissionType.ADMINS,
            selectedPermission = selectedPermission,
            onItemClicked = onPermissionChange
        )
        PermissionItem(
            channelAddPermissionType = ChannelAddPermissionType.EVERYONE,
            selectedPermission = selectedPermission,
            onItemClicked = onPermissionChange
        )
        Text(
            text = stringResource(id = R.string.channel_add_permission_description),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier.padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelAccessScreen() {
    WireTheme {
        ChannelAccessScreenContent(
            internalPadding = PaddingValues(),
            selectedAccess = ChannelAccessType.PRIVATE,
            selectedPermission = ChannelAddPermissionType.ADMINS
        )
    }
}
