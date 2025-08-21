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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.newconversation.groupOptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.destinations.ChannelAccessOnCreateScreenDestination
import com.wire.android.ui.destinations.ChannelHistoryScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.NewGroupConversationSearchPeopleScreenDestination
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelhistory.ChannelHistoryType
import com.wire.android.ui.home.newconversation.channelhistory.name
import com.wire.android.ui.home.newconversation.common.CreateGroupErrorDialog
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.CreateConversationParam
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

    LaunchedEffect(newConversationViewModel.createGroupState) {
        (newConversationViewModel.createGroupState as? CreateGroupState.Created)?.let {
            navigateToGroup(it.conversationId)
        }
    }

    GroupOptionScreenContent(
        groupOptionState = newConversationViewModel.groupOptionsState,
        createGroupState = newConversationViewModel.createGroupState,
        groupMetadataState = newConversationViewModel.newGroupState,
        onAccessClicked = {
            navigator.navigate(NavigationCommand(ChannelAccessOnCreateScreenDestination))
        },
        onHistoryClicked = {
            navigator.navigate(NavigationCommand(ChannelHistoryScreenDestination))
        },
        onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
        onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
        onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
        onContinuePressed =
            if (newConversationViewModel.newGroupState.isChannel) {
                newConversationViewModel::createChannel
            } else {
                newConversationViewModel::createGroup
            },
        onBackPressed = navigator::navigateBack,
        onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
        onNotAllowGuestsClicked = newConversationViewModel::onNotAllowGuestClicked,
        onAllowGuestsClicked = newConversationViewModel::onAllowGuestsClicked,
        onEditParticipantsClick = {
            newConversationViewModel.onCreateGroupErrorDismiss()
            navigator.navigate(NavigationCommand(NewGroupConversationSearchPeopleScreenDestination, BackStackMode.UPDATE_EXISTED))
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
private fun GroupOptionScreenContent(
    groupOptionState: GroupOptionState,
    createGroupState: CreateGroupState,
    groupMetadataState: GroupMetadataState,
    onAccessClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
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
    channelsHistoryOptionsEnabled: Boolean = BuildConfig.CHANNELS_HISTORY_OPTIONS_ENABLED,
    mlsReadReceiptsEnabled: Boolean = BuildConfig.MLS_READ_RECEIPTS_ENABLED,
) {
    with(groupOptionState) {
        WireScaffold(topBar = {
            val screenTitle = if (groupMetadataState.isChannel) {
                R.string.new_channel_title
            } else {
                R.string.new_group_title
            }
            val navigationIconType = if (groupMetadataState.isChannel) {
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
                groupMetadataState = groupMetadataState,
                channelsHistoryOptionsEnabled = channelsHistoryOptionsEnabled,
                mlsReadReceiptsEnabled = mlsReadReceiptsEnabled,
                onAccessClicked = onAccessClicked,
                onHistoryClicked = onHistoryClicked,
                onAllowGuestChanged = onAllowGuestChanged,
                onAllowServicesChanged = onAllowServicesChanged,
                onReadReceiptChanged = onReadReceiptChanged,
                onEnableWireCellChanged = onEnableWireCellChanged,
                onContinuePressed = onContinuePressed,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(internalPadding)
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        (createGroupState as? CreateGroupState.Error)?.let {
            CreateGroupErrorDialog(it, onErrorDismissed, onEditParticipantsClick, onDiscardGroupCreationClick)
        }
        if (showAllowGuestsDialog) {
            AllowGuestsDialog(onAllowGuestsDialogDismissed, onNotAllowGuestsClicked, onAllowGuestsClicked)
        }
    }
}

@Composable
private fun GroupOptionState.GroupOptionsScreenMainContent(
    groupMetadataState: GroupMetadataState,
    channelsHistoryOptionsEnabled: Boolean,
    mlsReadReceiptsEnabled: Boolean,
    onAccessClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onAllowGuestChanged: (Boolean) -> Unit,
    onAllowServicesChanged: (Boolean) -> Unit,
    onReadReceiptChanged: (Boolean) -> Unit,
    onEnableWireCellChanged: (Boolean) -> Unit,
    onContinuePressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (groupMetadataState.isChannel) {
                AccessOptions(groupMetadataState.channelAccessType, onAccessClicked)
                if (channelsHistoryOptionsEnabled) {
                    HistoryOptions(groupMetadataState.channelHistoryType, onHistoryClicked)
                }
            }
            AllowGuestsOptions(groupMetadataState.isChannel, onAllowGuestChanged)
            AllowServicesOptions(groupMetadataState.isChannel, onAllowServicesChanged)
            if (groupMetadataState.groupProtocol != CreateConversationParam.Protocol.MLS || mlsReadReceiptsEnabled) {
                ReadReceiptsOptions(groupMetadataState.isChannel, onReadReceiptChanged)
            }
            isWireCellsEnabled?.let {
                EnableWireCellOptions(onEnableWireCellChanged)
            }
        }
        CreateGroupButton(groupMetadataState.isChannel, onContinuePressed)
    }
}

@Composable
private fun GroupOptionState.ReadReceiptsOptions(isChannel: Boolean, onReadReceiptChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.read_receipts),
        switchState = SwitchState.Enabled(
            value = isReadReceiptEnabled,
            isOnOffVisible = false,
            onCheckedChange = onReadReceiptChanged,
        ),
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
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        style = typography().body01,
    )
}

@Composable
private fun GroupOptionState.AllowServicesOptions(isChannel: Boolean, onAllowServicesChanged: (Boolean) -> Unit) {
    if (!isTeamAllowedToUseApps) return

    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_services),
        switchState = SwitchState.Enabled(
            value = isAllowAppsEnabled,
            isOnOffVisible = false,
            onCheckedChange = onAllowServicesChanged,
        ),
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
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        style = typography().body01,
    )
}

@Composable
fun AccessOptions(
    accessType: ChannelAccessType,
    onAccessClicked: () -> Unit
) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.channel_access_label),
        arrowType = ArrowType.TITLE_ALIGNED,
        arrowLabel = stringResource(accessType.labelResId),
        clickable = Clickable(enabled = true, onClick = onAccessClicked),
    )
}

@Composable
fun HistoryOptions(
    historyType: ChannelHistoryType,
    onClicked: () -> Unit
) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.channel_history_label),
        arrowType = ArrowType.TITLE_ALIGNED,
        arrowLabel = historyType.name(useAmountForCustom = true),
        clickable = Clickable(enabled = true, onClick = onClicked),
    )
}

@Composable
private fun GroupOptionState.AllowGuestsOptions(isChannel: Boolean, onAllowGuestChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_guests),
        switchState = SwitchState.Enabled(
            value = isAllowGuestEnabled,
            isOnOffVisible = false,
            onCheckedChange = onAllowGuestChanged
        ),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )

    if (!isChannel) {
        Text(
            text = stringResource(R.string.allow_guest_switch_description),
            color = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
            textAlign = TextAlign.Left,
            style = typography().body01,
        )
    }
}

@Composable
private fun GroupOptionState.EnableWireCellOptions(onEnableWireCell: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.enable_wire_cell),
        switchState = SwitchState.Enabled(
            value = isWireCellsEnabled ?: false,
            isOnOffVisible = false,
            onCheckedChange = { onEnableWireCell.invoke(it) }
        ),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )

    Text(
        text = stringResource(R.string.enable_wire_cell_switch_description),
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        style = typography().body01,
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
private fun PreviewGroupOptionScreen(
    groupMetadataState: GroupMetadataState,
    channelsHistoryOptionsEnabled: Boolean = BuildConfig.CHANNELS_HISTORY_OPTIONS_ENABLED,
    mlsReadReceiptsEnabled: Boolean = BuildConfig.MLS_READ_RECEIPTS_ENABLED,
) = WireTheme {
    GroupOptionScreenContent(
        groupOptionState = GroupOptionState(),
        createGroupState = CreateGroupState.Default,
        groupMetadataState = groupMetadataState,
        onAccessClicked = {},
        onHistoryClicked = {},
        onAllowGuestChanged = {},
        onAllowServicesChanged = {},
        onReadReceiptChanged = {},
        onEnableWireCellChanged = {},
        onContinuePressed = {},
        onAllowGuestsDialogDismissed = {},
        onNotAllowGuestsClicked = {},
        onAllowGuestsClicked = {},
        onErrorDismissed = {},
        onEditParticipantsClick = {},
        onDiscardGroupCreationClick = {},
        onBackPressed = {},
        channelsHistoryOptionsEnabled = channelsHistoryOptionsEnabled,
        mlsReadReceiptsEnabled = mlsReadReceiptsEnabled,
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupOptionScreen_Group() = PreviewGroupOptionScreen(
    groupMetadataState = GroupMetadataState(isChannel = false)
)

@Composable
@PreviewMultipleThemes
fun PreviewGroupOptionScreen_Channel() = PreviewGroupOptionScreen(
    groupMetadataState = GroupMetadataState(isChannel = true),
    channelsHistoryOptionsEnabled = true,
)

@Composable
@PreviewMultipleThemes
fun PreviewGroupOptionScreen_ChannelWithHistoryOptionsDisabled() = PreviewGroupOptionScreen(
    groupMetadataState = GroupMetadataState(isChannel = true),
    channelsHistoryOptionsEnabled = false,
)

@Composable
@PreviewMultipleThemes
fun PreviewGroupOptionScreen_MlsGroupWithMlsReadReceiptsDisabled() = PreviewGroupOptionScreen(
    groupMetadataState = GroupMetadataState(
        isChannel = false,
        groupProtocol = CreateConversationParam.Protocol.MLS,
    ),
    mlsReadReceiptsEnabled = false,
)
