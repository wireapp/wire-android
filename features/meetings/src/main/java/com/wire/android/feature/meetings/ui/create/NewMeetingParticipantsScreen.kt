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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.navigation.MeetingNavigator
import com.wire.android.model.ItemActionType
import com.wire.android.navigation.annotation.features.meetings.WireNewMeetingDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.search.SearchUsersAndAppsScreen
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.user.UserId
import com.wire.android.ui.common.R as commonR

@WireNewMeetingDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun NewMeetingParticipantsScreen(
    navigator: MeetingNavigator,
    newMeetingViewModel: NewMeetingViewModel,
) {
    SearchUsersAndAppsScreen(
        onlyConnectedContacts = true,
        searchTitle = stringResource(R.string.new_meeting_participants_title),
        selectedContacts = newMeetingViewModel.state.selectedContacts,
        onContactChecked = newMeetingViewModel::updateSelectedContact,
        onClose = {
            newMeetingViewModel.resetSelectedContacts()
            navigator.navigateBack()
        },
        navigationIconType = NavigationIconType.Back(R.string.content_description_new_meeting_participants_back_icon),
        itemActionType = ItemActionType.CHECK,
        isAppsTabVisible = false,
        onOpenUserProfile = { contact ->
            navigator.navigateToProfile(UserId(contact.id, contact.domain))
        },
        peopleBottomActions = { focusRequester ->
            SelectButton(
                onClick = {
                    newMeetingViewModel.confirmSelectedContacts()
                    navigator.navigateBack()
                },
                buttonModifier = Modifier.focusRequester(focusRequester),
            )
        }
    )
}

@Composable
private fun SelectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
) {
    Surface(
        color = MaterialTheme.wireColorScheme.background,
        shadowElevation = elevation
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(horizontal = dimensions().spacing16x)
                .height(dimensions().groupButtonHeight)
        ) {
            WirePrimaryButton(
                text = stringResource(commonR.string.label_select),
                leadingIcon = leadingIcon,
                onClick = onClick,
                state = WireButtonState.Default,
                modifier = buttonModifier,
            )
        }
    }
}
