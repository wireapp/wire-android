package com.wire.android.ui.home.conversations.details.options

import androidx.annotation.StringRes
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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun GroupConversationOptions(
    groupOptionsState: GroupConversationOptionsState,
    onGuestUpdate: (Boolean) -> Unit,
    onServicesUpdate: (Boolean) -> Unit,
    onGuestDialogConfirm: () -> Unit,
    onGuestDialogDismiss: () -> Unit,
    lazyListState: LazyListState
) {
    GroupConversationSettings(
        state = groupOptionsState,
        onGuestSwitchClicked = onGuestUpdate,
        onServiceSwitchClicked = onServicesUpdate,
        lazyListState = lazyListState
    )
    DisableGuestConformationDialog(
        state = groupOptionsState.isGuestUpdateDialogShown,
        onConform = onGuestDialogConfirm,
        onDialogDismiss = onGuestDialogDismiss
    )
}

@Composable
fun GroupConversationSettings(
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
            item { FolderHeader(name = stringResource(R.string.folder_lable_access)) }

            item {
                GuestOption(
                    isClickable = state.isUpdatingGuestAllowed,
                    switchState = state.isGuestAllowed,
                    onCheckedChange = onGuestSwitchClicked
                )
            }

            item {
                ServicesOption(
                    isClickable = state.isUpdatingAllowed,
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
private fun GuestOption(isClickable: Boolean, switchState: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GroupOptionWithSwitch(
        isClickable = isClickable,
        switchState = switchState,
        onClick = onCheckedChange,
        title = R.string.label_membership_guest,
        subTitle = R.string.convrsation_options_guest_discriptions
    )
}

@Composable
private fun ServicesOption(isClickable: Boolean, switchState: Boolean, onCheckedChange: (Boolean) -> Unit) {
    GroupOptionWithSwitch(
        isClickable = isClickable,
        switchState = switchState,
        onClick = onCheckedChange,
        title = R.string.conversation_Option_services_lable,
        subTitle = R.string.convrsation_options_services_discriptions
    )
}

@Composable
private fun GroupOptionWithSwitch(
    isClickable: Boolean,
    switchState: Boolean,
    onClick: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes subTitle: Int?
) {
    GroupConversationOptionsItem(
        title = stringResource(id = title),
        subtitle = when (isClickable) {
            true -> subTitle?.let { stringResource(id = it) }
            false -> null
        },
        switchState = if (isClickable) SwitchState.Enabled(
            value = switchState,
            onCheckedChange = onClick
        ) else SwitchState.TextOnly(switchState),
        arrowType = ArrowType.NONE
    )
    Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
}

@Composable
private fun DisableGuestConformationDialog(state: Boolean, onConform: () -> Unit, onDialogDismiss: () -> Unit) {
    if (state) {
        WireDialog(
            title = stringResource(id = R.string.disable_guest_dialog_title),
            text = stringResource(id = R.string.disable_guest_dialog_text),
            onDismiss = onDialogDismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onDialogDismiss,
                text = stringResource(id = R.string.label_cancel),
                type = WireDialogButtonType.Secondary,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = onConform,
                text = stringResource(id = R.string.label_disable),
                type = WireDialogButtonType.Primary,
            )
        )
    }
}

@Preview
@Composable
private fun TeamGroupConversationOptionsPreview() {
    GroupConversationSettings(
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
    GroupConversationSettings(
        GroupConversationOptionsState(
            groupName = "Normal Group Conversation",
            isUpdatingAllowed = true,
            isTeamGroup = false
        ), {}, {}

    )
}

@Preview
@Composable
private fun DisableGuestConformationDialogPreview() {
    DisableGuestConformationDialog(true, {}, {})
}

