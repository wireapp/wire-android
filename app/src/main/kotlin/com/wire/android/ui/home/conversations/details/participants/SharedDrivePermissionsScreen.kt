/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun SharedDrivePermissionsRouteScreen(
    viewModel: GroupConversationParticipantsViewModel,
    isSelfUserViewerOnly: Boolean,
    onBackPressed: () -> Unit,
) {
    SharedDrivePermissionsContent(
        groupParticipantsState = viewModel.groupParticipantsState,
        isSelfUserViewerOnly = isSelfUserViewerOnly,
        onBackPressed = onBackPressed,
    )
}

@Composable
private fun SharedDrivePermissionsContent(
    groupParticipantsState: GroupConversationParticipantsState,
    isSelfUserViewerOnly: Boolean,
    onBackPressed: () -> Unit,
) {
    val members = groupParticipantsState.data.admins + groupParticipantsState.data.participants
    val lazyListState = rememberLazyListState()

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.shared_drive_permissions_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onBackPressed,
            )
        },
        modifier = Modifier.fillMaxHeight(),
    ) { internalPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(internalPadding),
        ) {
            item {
                GroupConversationOptionsItem(
                    title = stringResource(R.string.conversation_options_shared_drive_label),
                    subtitle = stringResource(R.string.conversation_options_shared_drive_description),
                switchState = SwitchState.Disabled(value = true),
                arrowType = com.wire.android.ui.home.conversations.details.options.ArrowType.NONE,
                clickable = Clickable(enabled = false),
                )
            }
            item {
                SectionHeader(
                    name = stringResource(R.string.shared_drive_permissions_access_header),
                    padding = PaddingValues(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing8x,
                    ),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .background(colorsScheme().surface)
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing8x,
                        )
                ) {
                    Text(
                        text = stringResource(R.string.shared_drive_permissions_editors_description),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.secondaryText,
                    )
                    Text(
                        text = stringResource(R.string.shared_drive_permissions_viewers_description),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.secondaryText,
                        modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x),
                    )
                    Text(
                        text = stringResource(R.string.shared_drive_permissions_external_description),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.secondaryText,
                        modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing16x),
                    )
                }
            }
            items(members, key = { it.id.toString() }) { participant ->
                ConversationParticipantItem(
                    uiParticipant = participant,
                    clickable = Clickable(enabled = false),
                    showRightArrow = false,
                    showVerificationLabels = false,
                    trailingContent = {
                        Text(
                            text = stringResource(
                                if (participant.membership in setOf(
                                        Membership.Guest,
                                        Membership.External,
                                        Membership.Federated,
                                    )
                                ) {
                                    R.string.shared_drive_permissions_viewer
                                } else {
                                    R.string.shared_drive_permissions_editor
                                }
                            ),
                            style = MaterialTheme.wireTypography.body01,
                            color = MaterialTheme.wireColorScheme.secondaryText,
                        )
                    },
                )
                WireDivider()
            }
        }
    }
}
