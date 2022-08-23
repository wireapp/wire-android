package com.wire.android.ui.home.newconversation.groupOptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.DialogErrorStrings

@Composable
fun GroupOptionScreen(
    groupOptionState: GroupOptionState,
    onAllowGuestChanged: ((Boolean) -> Unit),
    onAllowServicesChanged: ((Boolean) -> Unit),
    onReadReceiptChanged: ((Boolean) -> Unit),
    onCreateGroup: () -> Unit,
    onAllowGuestsDialogDismissed: () -> Unit,
    onNotAllowGuestsClicked: () -> Unit,
    onAllowGuestsClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    GroupOptionScreenContent(
        groupOptionState = groupOptionState,
        onAllowGuestChanged = onAllowGuestChanged,
        onAllowServicesChanged = onAllowServicesChanged,
        onReadReceiptChanged = onReadReceiptChanged,
        onContinuePressed = onCreateGroup,
        onBackPressed = onBackPressed,
        onAllowGuestsDialogDismissed = onAllowGuestsDialogDismissed,
        onNotAllowGuestsClicked = onNotAllowGuestsClicked,
        onAllowGuestsClicked = onAllowGuestsClicked,
        onErrorDismissed = onErrorDismissed,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun GroupOptionScreenContent(
    groupOptionState: GroupOptionState,
    onAllowGuestChanged: ((Boolean) -> Unit),
    onAllowServicesChanged: ((Boolean) -> Unit),
    onReadReceiptChanged: ((Boolean) -> Unit),
    onContinuePressed: () -> Unit,
    onAllowGuestsDialogDismissed: () -> Unit,
    onNotAllowGuestsClicked: () -> Unit,
    onAllowGuestsClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(groupOptionState) {
        Scaffold(topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                title = stringResource(id = R.string.new_group_title)
            )
        }) { internalPadding ->
            GroupOptionsScreenMainContent(
                internalPadding,
                onAllowGuestChanged,
                onAllowServicesChanged,
                onReadReceiptChanged,
                onContinuePressed
            )
        }

        error?.let {
            ErrorDialog(it, onErrorDismissed)
        }
        if (showAllowGuestsDialog) {
            AllowGuestsDialog(onAllowGuestsDialogDismissed, onNotAllowGuestsClicked, onAllowGuestsClicked)
        }
    }
}

@Composable
private fun GroupOptionState.GroupOptionsScreenMainContent(
    internalPadding: PaddingValues,
    onAllowGuestChanged: (Boolean) -> Unit,
    onAllowServicesChanged: (Boolean) -> Unit,
    onReadReceiptChanged: (Boolean) -> Unit,
    onContinuePressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(internalPadding)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column() {
            AllowGuestsOptions(onAllowGuestChanged)
            AllowServicesOptions(onAllowServicesChanged)
            ReadReceiptsOptions(onReadReceiptChanged)
        }
        ContinueButton(onContinuePressed)
    }
}

@Composable
private fun GroupOptionState.ReadReceiptsOptions(onReadReceiptChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.read_receipts),
        switchState = SwitchState.Enabled(value = isReadReceiptEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onReadReceiptChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}, onLongClick = {}),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    )

    Text(
        text = stringResource(R.string.read_receipts_description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
private fun GroupOptionState.AllowServicesOptions(onAllowServicesChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_services),
        switchState = SwitchState.Enabled(value = isAllowServicesEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onAllowServicesChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}, onLongClick = {}),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    )

    Text(
        text = stringResource(R.string.allow_services_description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
private fun GroupOptionState.AllowGuestsOptions(onAllowGuestChanged: (Boolean) -> Unit) {
    GroupConversationOptionsItem(
        title = stringResource(R.string.allow_guests),
        switchState = SwitchState.Enabled(value = isAllowGuestEnabled,
            isOnOffVisible = false,
            onCheckedChange = { onAllowGuestChanged.invoke(it) }),
        arrowType = ArrowType.NONE,
        clickable = Clickable(enabled = false, onClick = {}, onLongClick = {}),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )

    Text(
        text = stringResource(R.string.allow_guest_switch_description),
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x),
        textAlign = TextAlign.Left,
        fontSize = 16.sp
    )
}

@Composable
private fun GroupOptionState.ContinueButton(
    onContinuePressed: () -> Unit
) {
    WirePrimaryButton(
        text = stringResource(R.string.label_continue),
        onClick = onContinuePressed,
        fillMaxWidth = true,
        loading = isLoading,
        trailingIcon = if(isLoading) null else Icons.Filled.ChevronRight.Icon(),
        state = if (continueEnabled && !isLoading) WireButtonState.Default else WireButtonState.Disabled,
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.wireDimensions.spacing16x)
    )
}

@Composable
private fun AllowGuestsDialog(
    onAllowGuestsDialogDismissed: () -> Unit,
    onNotAllowGuestsClicked: () -> Unit,
    onAllowGuestsClicked: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.disable_guests_dialoug_title),
        text = stringResource(R.string.disable_guests_dialoug_description),
        onDismiss = onAllowGuestsDialogDismissed,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onNotAllowGuestsClicked,
            text = stringResource(id = R.string.disable_guests_dialoug_button),
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
private fun ErrorDialog(error: GroupOptionState.Error, onErrorDismissed: () -> Unit) {
    val dialogStrings = when (error) {
        is GroupOptionState.Error.LackingConnection -> DialogErrorStrings(
            stringResource(R.string.error_no_network_title),
            stringResource(R.string.error_no_network_message)
        )

        is GroupOptionState.Error.Unknown -> DialogErrorStrings(
            stringResource(R.string.error_unknown_title),
            stringResource(R.string.error_unknown_message)
        )
    }
    WireDialog(
        dialogStrings.title, dialogStrings.message,
        onDismiss = onErrorDismissed,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onErrorDismissed,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
@Preview
private fun GroupOptionScreenPreview() {
    GroupOptionScreenContent(
        GroupOptionState(),
        {}, {}, {}, {}, {}, {}, {}, {}, {}
    )
}
