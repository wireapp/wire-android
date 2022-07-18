package com.wire.android.ui.home.conversations.details.options

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun GroupConversationOptionsScreen(
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    lazyListState: LazyListState
) {
    GroupConversationOptions(
        state = viewModel.groupOptionsState,
        onGuestSwitchClicked = viewModel::onAccessUpdate,
        onServiceSwitchClicked = viewModel::onServicesUpdate,
        lazyListState = lazyListState
    )
}

@Composable
fun GroupConversationOptions(
    state: GroupConversationOptionsState,
    onGuestSwitchClicked: (Boolean) -> Unit,
    onServiceSwitchClicked: (Boolean) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GroupNameItem(groupName = state.groupName, canBeChanged = state.isUpdatingAllowed)
        }
        if (state.isTeamGroup) {
            item { FolderHeader(name = "Access") }

            item {
                GuestOption(
                    canBeChanged = state.isUpdatingGuestAllowed,
                    switchState = state.isGuestAllowed,
                    onCheckedChange = onGuestSwitchClicked
                )
            }

            item {
                ServicesOption(
                    canBeChanged = state.isUpdatingAllowed,
                    switchState = state.isServicesAllowed,
                    onCheckedChange = onServiceSwitchClicked
                )
            }
        }
    }
}

@Composable
private fun GroupNameItem(groupName: String, canBeChanged: Boolean) {
    GroupConversationOptionsItem(
        label = stringResource(id = R.string.conversation_details_options_group_name),
        title = groupName,
        clickable = Clickable(enabled = canBeChanged, onClick = { /* TODO */ }, onLongClick = { /* not handled */ })
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}


@Composable
private fun GuestOption(canBeChanged: Boolean, switchState: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(id = R.string.label_membership_guest),
        subtitle = stringResource(id = R.string.convrsation_options_guest_discriptions),
        switchState = if (canBeChanged) SwitchState.Enabled(switchState, onCheckedChange) else SwitchState.Disabled(switchState),
        arrowType = ArrowType.NONE
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}

@Composable
private fun ServicesOption(canBeChanged: Boolean, switchState: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(id = R.string.conversation_Option_services_lable),
        subtitle = stringResource(id = R.string.convrsation_options_services_discriptions),
        switchState = if (canBeChanged) SwitchState.Enabled(switchState, onCheckedChange) else SwitchState.Disabled(switchState),
        arrowType = ArrowType.NONE
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}


@Preview
@Composable
private fun TeamGroupConversationOptionsPreview() {
    GroupConversationOptions(
        GroupConversationOptionsState(
            groupName = "Team Group Conversation",
            isUpdatingAllowed = true,
            isTeamGroup = true,
            isGuestAllowed = true
        ),
        {}, {}
    )
}

@Preview
@Composable
private fun NormalGroupConversationOptionsPreview() {
    GroupConversationOptions(
        GroupConversationOptionsState(
            groupName = "Normal Group Conversation",
            isUpdatingAllowed = true,
            isTeamGroup = false
        ), {}, {}

    )
}
