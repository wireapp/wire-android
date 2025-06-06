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

package com.wire.android.ui.common.dialogs

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MaxAccountsReachedDialog(
    dialogState: VisibilityState<MaxAccountsReachedDialogState>,
    onActionButtonClicked: () -> Unit,
) {
    VisibilityState(dialogState) { _ ->
        MaxAccountAllowedDialogContent(
            onConfirm = onActionButtonClicked,
            onDismiss = dialogState::dismiss,
            buttonText = R.string.label_ok,
            dialogProperties = wireDialogPropertiesBuilder(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}

@Composable
fun MaxAccountAllowedDialogContent(
    @StringRes buttonText: Int,
    onConfirm: () -> Unit,
    maxAccountsAllowed: Int = BuildConfig.MAX_ACCOUNTS,
    @PluralsRes title: Int = R.plurals.max_account_reached_dialog_title,
    @PluralsRes message: Int = R.plurals.max_account_reached_dialog_message,
    dialogProperties: DialogProperties = wireDialogPropertiesBuilder(),
    onDismiss: () -> Unit
) {
    WireDialog(
        title = pluralStringResource(title, maxAccountsAllowed, maxAccountsAllowed),
        text = pluralStringResource(message, maxAccountsAllowed, maxAccountsAllowed),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(buttonText),
            onClick = onConfirm,
            type = WireDialogButtonType.Primary
        ),
        properties = dialogProperties
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMaxAccountReachedDialogWithOkButton() {
    WireTheme {
        MaxAccountAllowedDialogContent(onConfirm = { }, onDismiss = { }, buttonText = R.string.label_ok)
    }
}
