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
package com.wire.android.ui.home.conversations.promoteadmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ItemActionType
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.promoteAdminViewModel
import com.wire.android.ui.home.conversations.search.InternalContactSearchResultItem
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as commonR

@WireRootDestination(
    navArgs = PromoteAdminNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun PromoteAdminScreen(
    navigator: Navigator,
    viewModel: PromoteAdminViewModel = promoteAdminViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val failedToPromoteMessage = stringResource(R.string.promote_admin_error_failed_to_promote)
    val failedToLeaveMessage = stringResource(R.string.promote_admin_error_failed_to_leave)

    PromoteAdminContent(
        state = state,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onUserSelected = viewModel::onUserSelected,
        onPromoteAdminAndLeave = viewModel::onPromoteAdminAndLeave,
        onClose = navigator::navigateBack,
    )

    HandleActions(viewModel.actions) { action ->
        when (action) {
            PromoteAdminAction.Success -> navigator.navigateBack()
            PromoteAdminAction.FailedToPromoteUser ->
                coroutineScope.launch { snackbarHostState.showSnackbar(failedToPromoteMessage) }
            PromoteAdminAction.FailedToLeaveConversation ->
                coroutineScope.launch { snackbarHostState.showSnackbar(failedToLeaveMessage) }
        }
    }
}

@Composable
private fun PromoteAdminContent(
    state: PromoteAdminState,
    onSearchQueryChanged: (String) -> Unit,
    onUserSelected: (UserId) -> Unit,
    onPromoteAdminAndLeave: () -> Unit,
    onClose: () -> Unit,
) {
    val searchTextState = rememberTextFieldState()

    LaunchedEffect(Unit) {
        snapshotFlow { searchTextState.text.toString() }
            .collect { onSearchQueryChanged(it) }
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.promote_admin_screen_title),
                navigationIconType = NavigationIconType.Close(R.string.content_description_close_button),
                elevation = dimensions().spacing0x,
                onNavigationPressed = onClose,
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x)
                        .height(dimensions().groupButtonHeight),
                ) {
                    WirePrimaryButton(
                        text = stringResource(R.string.promote_admin_button),
                        onClick = onPromoteAdminAndLeave,
                        state = if (state.isButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
                    )
                }
            }
        },
    ) { internalPadding ->
        LazyColumn(modifier = Modifier.padding(internalPadding)) {
            item {
                SearchBarInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensions().spacing16x,
                            vertical = dimensions().spacing8x,
                        ),
                    placeholderText = stringResource(R.string.promote_admin_search_placeholder),
                    leadingIcon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(start = dimensions().spacing4x)
                                .size(dimensions().buttonCircleMinSize)
                        ) {
                            Icon(
                                painter = painterResource(id = commonR.drawable.ic_search),
                                contentDescription = stringResource(commonR.string.content_description_conversation_search_icon),
                                tint = MaterialTheme.wireColorScheme.onBackground
                            )
                        }
                    },
                    textState = searchTextState,
                )
            }
            items(state.filteredMembers, key = { it.userId.toString() }) { member ->
                InternalContactSearchResultItem(
                    avatarData = member.avatarData,
                    name = member.name,
                    label = member.handle,
                    membership = Membership.Standard,
                    searchQuery = state.searchQuery,
                    connectionState = ConnectionState.ACCEPTED,
                    isSelected = state.selectedUserId == member.userId,
                    actionType = ItemActionType.CHECK,
                    onCheckClickable = Clickable { onUserSelected(member.userId) },
                    clickable = Clickable { onUserSelected(member.userId) },
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewPromoteAdminScreen() = WireTheme {
    PromoteAdminContent(
        state = PromoteAdminState(
            filteredMembers = listOf(
                PromoteAdminMemberItem(
                    userId = UserId("user1", "wire.com"),
                    name = "Alice",
                    handle = "@alice",
                ),
                PromoteAdminMemberItem(
                    userId = UserId("user2", "wire.com"),
                    name = "Bob",
                    handle = "@bob",
                ),
            ),
            selectedUserId = UserId("user1", "wire.com"),
        ),
        onSearchQueryChanged = {},
        onUserSelected = {},
        onPromoteAdminAndLeave = {},
        onClose = {},
    )
}
