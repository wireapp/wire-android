/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState
import com.wire.android.ui.markdown.MarkdownConsts
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.DialogAnnotatedErrorStrings

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
    onEditParticipantsClick: () -> Unit,
    onDiscardGroupCreationClick: () -> Unit,
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
        onEditParticipantsClick = onEditParticipantsClick,
        onDiscardGroupCreationClick = onDiscardGroupCreationClick,
        onErrorDismissed = onErrorDismissed
    )
}

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
    onEditParticipantsClick: () -> Unit,
    onDiscardGroupCreationClick: () -> Unit,
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
            ErrorDialog(it, onErrorDismissed, onEditParticipantsClick, onDiscardGroupCreationClick)
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
        trailingIcon = if (isLoading) null else Icons.Filled.ChevronRight.Icon(),
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
private fun ErrorDialog(
    error: GroupOptionState.Error,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val dialogStrings = when (error) {
        is GroupOptionState.Error.LackingConnection -> DialogAnnotatedErrorStrings(
            stringResource(R.string.error_no_network_title),
            buildAnnotatedString { append(stringResource(R.string.error_no_network_message)) }
        )

        is GroupOptionState.Error.Unknown -> DialogAnnotatedErrorStrings(
            stringResource(R.string.error_unknown_title),
            buildAnnotatedString { append(stringResource(R.string.error_unknown_message)) }
        )

        is GroupOptionState.Error.ConflictedBackends -> DialogAnnotatedErrorStrings(
            title = stringResource(id = R.string.group_can_not_be_created_title),
            annotatedMessage = buildAnnotatedString {
                val description = stringResource(
                    id = R.string.group_can_not_be_created_federation_conflict_description,
                    error.domains.dropLast(1).joinToString(", "),
                    error.domains.last()
                )
                val learnMore = stringResource(id = R.string.label_learn_more)

                append(description)
                append(' ')

                withStyle(
                    style = SpanStyle(
                        color = colorsScheme().primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(learnMore)
                }
                addStringAnnotation(
                    tag = MarkdownConsts.TAG_URL,
                    annotation = stringResource(id = R.string.url_support),
                    start = description.length + 1,
                    end = description.length + 1 + learnMore.length
                )
            }
        )
    }

    WireDialog(
        dialogStrings.title,
        dialogStrings.annotatedMessage,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = if (error.isConflictedBackends) onAccept else onDismiss,
            text = stringResource(
                id = if (error.isConflictedBackends) {
                    R.string.group_can_not_be_created_edit_participiant_list
                } else {
                    R.string.label_ok
                }
            ),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onCancel,
            text = stringResource(
                id = if (error is GroupOptionState.Error.ConflictedBackends) {
                    R.string.group_can_not_be_created_discard_group_creation
                } else {
                    R.string.label_ok
                }
            ),
            type = WireDialogButtonType.Secondary,
        )
    )
}

@Composable
@Preview
fun PreviewGroupOptionScreen() {
    GroupOptionScreenContent(
        GroupOptionState(),
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
    )
}
