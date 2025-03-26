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

package com.wire.android.ui.home.newconversation.groupOptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.ChannelAccessScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.CreateGroupErrorDialog
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId

@NewConversationNavGraph
@WireDestination
@Composable
fun GroupOptionScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel,
) {
    fun navigateToGroup(conversationId: ConversationId): Unit =
        navigator.navigate(NavigationCommand(ConversationScreenDestination(conversationId), BackStackMode.REMOVE_CURRENT_NESTED_GRAPH))

    GroupOptionScreenContent(
        groupOptionState = newConversationViewModel.groupOptionsState,
        createGroupState = newConversationViewModel.createGroupState,
        accessTypeLabel = newConversationViewModel.newGroupState.channelAccessType.label,
        isChannelsAllowed = newConversationViewModel.newGroupState.isChannel,
        onAccessClicked = {
            navigator.navigate(NavigationCommand(ChannelAccessScreenDestination))
        },
        onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
        onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
        onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
        onContinuePressed = {
            if (newConversationViewModel.newGroupState.isChannel) {
                newConversationViewModel.createChannel(::navigateToGroup)
            } else {
                newConversationViewModel.createGroup(::navigateToGroup)
            }
        },
        onBackPressed = navigator::navigateBack,
        onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
        onNotAllowGuestsClicked = { newConversationViewModel.onNotAllowGuestClicked(::navigateToGroup) },
        onAllowGuestsClicked = { newConversationViewModel.onAllowGuestsClicked(::navigateToGroup) },
        onEditParticipantsClick = {
            newConversationViewModel.onCreateGroupErrorDismiss()
            navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination, BackStackMode.UPDATE_EXISTED))
        },
        onDiscardGroupCreationClick = {
            newConversationViewModel.onCreateGroupErrorDismiss()
            navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
        },
        onErrorDismissed = newConversationViewModel::onCreateGroupErrorDismiss,
        onEnableWireCellChanged = newConversationViewModel::onEnableWireCellChanged
    )
}

@Composable
fun GroupOptionScreenContent(
    groupOptionState: GroupOptionState,
    createGroupState: CreateGroupState,
    accessTypeLabel: Int,
    isChannelsAllowed: Boolean,
    onAccessClicked: () -> Unit,
    onAllowGuestChanged: ((Boolean) -> Unit),
    onAllowServicesChanged: ((Boolean) -> Unit),
    onReadReceiptChanged: ((Boolean) -> Unit),
    onEnableWireCellChanged: ((Boolean) -> Unit),
    onContinuePressed: () -> Unit,
    onAllowGuestsDialogDismissed: () -> Unit,
    onNotAllowGuestsClicked: () -> Unit,
    onAllowGuestsClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
    onEditParticipantsClick: () -> Unit,
    onDiscardGroupCreationClick: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(groupOptionState) {
        WireScaffold(topBar = {
            val screenTitle = if (isChannelsAllowed) {
                R.string.new_channel_title
            } else {
                R.string.new_group_title
            }
            val navigationIconType = if (isChannelsAllowed) {
                NavigationIconType.Back(R.string.content_description_new_channel_options_back_btn)
            } else {
                NavigationIconType.Back(R.string.content_description_new_group_options_back_btn)
            }
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = screenTitle),
                titleContentDescription = stringResource(id = R.string.content_description_new_conversation_options_heading),
                navigationIconType = navigationIconType
            )
        }) { internalPadding ->
            GroupOptionsScreenMainContent(
                accessTypeLabel,
                isChannelsAllowed,
                internalPadding,
                onAccessClicked,
                onAllowGuestChanged,
                onAllowServicesChanged,
                onReadReceiptChanged,
                onEnableWireCellChanged,
                onContinuePressed
            )
        }

        createGroupState.error?.let {
            CreateGroupErrorDialog(it, onErrorDismissed, onEditParticipantsClick, onDiscardGroupCreationClick)
        }
        if (showAllowGuestsDialog) {
            AllowGuestsDialog(onAllowGuestsDialogDismissed, onNotAllowGuestsClicked, onAllowGuestsClicked)
        }
    }
}

@Composable
private fun GroupOptionState.GroupOptionsScreenMainContent(
    accessTypeLabel: Int,
    isChannel: Boolean,
    internalPadding: PaddingValues,
    onAccessClicked: () -> Unit,
    onAllowGuestChanged: (Boolean) -> Unit,
    onAllowServicesChanged: (Boolean) -> Unit,
    onReadReceiptChanged: (Boolean) -> Unit,
    onEnableWireCellChanged: (Boolean) -> Unit,
    onContinuePressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(internalPadding)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (isChannel) {
                AccessOptions(accessTypeLabel, onAccessClicked)
            }
            AllowGuestsOptions(isChannel, onAllowGuestChanged)
            AllowServicesOptions(isChannel, onAllowServicesChanged)
            ReadReceiptsOptions(isChannel, onReadReceiptChanged)
            isWireCellsEnabled?.let {
                EnableWireCellOptions(onEnableWireCellChanged)
            }
        }
        CreateGroupButton(isChannel, onContinuePressed)
    }
}

@Composable
private fun GroupOptionState.ReadReceiptsOptions(isChannel: Boolean, onReadReceiptChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.read_receipts),
        switchState = SwitchState.Enabled(value = isReadReceiptEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onReadReceiptChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    )
    val description = if (isChannel) {
        R.string.read_receipts_channel_description
    } else {
        R.string.read_receipts_regular_group_description
    }
    Text(
        text = stringResource(description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
private fun GroupOptionState.AllowServicesOptions(isChannel: Boolean, onAllowServicesChanged: (Boolean) -> Unit) {
    if (!isAllowServicesPossible) return

    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_services),
        switchState = SwitchState.Enabled(value = isAllowServicesEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onAllowServicesChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    )

    val description = if (isChannel) {
        R.string.allow_services_channel_description
    } else {
        R.string.allow_services_regular_group_description
    }
    Text(
        text = stringResource(description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
fun AccessOptions(
    accessTypeLabel: Int,
    onAccessClicked: () -> Unit
) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.channel_access_label),
        arrowType = ArrowType.TITLE_ALIGNED,
        arrowLabel = stringResource(accessTypeLabel),
        onClick = onAccessClicked,
        isClickable = true,
    )
}

@Composable
private fun GroupOptionState.AllowGuestsOptions(isChannel: Boolean, onAllowGuestChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_guests),
        switchState = SwitchState.Enabled(value = isAllowGuestEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onAllowGuestChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )

    if (!isChannel) {
        Text(
            text = stringResource(R.string.allow_guest_switch_description),
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
            textAlign = TextAlign.Left,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun GroupOptionState.EnableWireCellOptions(onEnableWireCell: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.enable_wire_cell),
        switchState = SwitchState.Enabled(value = isWireCellsEnabled ?: false,
            isOnOffVisible = false,
            onCheckedChange = { onEnableWireCell.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )

    Text(
        text = stringResource(R.string.enable_wire_cell_switch_description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
private fun GroupOptionState.CreateGroupButton(
    isChannel: Boolean,
    onCreate: () -> Unit
) {
    val buttonLabel = if (isChannel) {
        R.string.create_channel_button_label
    } else {
        R.string.create_regular_group_button_label
    }
    WirePrimaryButton(
        text = stringResource(buttonLabel),
        onClick = onCreate,
        fillMaxWidth = true,
        loading = isLoading,
        state = if (continueEnabled && !isLoading) WireButtonState.Default else WireButtonState.Disabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.wireDimensions.spacing16x)
    )
}

@Composable
private fun AllowGuestsDialog(
    onAllowGuestsDialogDismissed: () -> Unit,
    onNotAllowGuestsClicked: () -> Unit,
    onAllowGuestsClicked: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.disable_guests_dialog_title),
        text = stringResource(R.string.disable_guests_dialog_description),
        onDismiss = onAllowGuestsDialogDismissed,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onNotAllowGuestsClicked,
            text = stringResource(id = R.string.disable_guests_dialog_button),
            type = WireDialogButtonType.Primary
        ), optionButton2Properties = WireDialogButtonProperties(
            text = stringResource(R.string.allow_guests),
            onClick = onAllowGuestsClicked,
            type = WireDialogButtonType.Primary
        ), dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.label_cancel),
            onClick = onAllowGuestsDialogDismissed
        )
    )
}

@Composable
@Preview
fun PreviewGroupOptionScreen() {
    GroupOptionScreenContent(
        GroupOptionState(),
        CreateGroupState(),
        accessTypeLabel = R.string.channel_private_label,
        isChannelsAllowed = false,
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
    )
}
