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

package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder

@Composable
fun MaxAccountsReachedDialogContent(
    maxAccountsAllowed: Int = BuildConfig.MAX_ACCOUNTS,
    dialogState: VisibilityState<MaxAccountsReachedDialogState>,
    onActionButtonClicked: () -> Unit,
) {
    VisibilityState(dialogState) { _ ->
        WireDialog(
            title = pluralStringResource(R.plurals.max_account_reached_dialog_title, maxAccountsAllowed),
            text = pluralStringResource(R.plurals.max_account_reached_dialog_message, maxAccountsAllowed),
            onDismiss = dialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_ok),
                onClick = onActionButtonClicked,
                type = WireDialogButtonType.Primary
            ),
            properties = wireDialogPropertiesBuilder(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
