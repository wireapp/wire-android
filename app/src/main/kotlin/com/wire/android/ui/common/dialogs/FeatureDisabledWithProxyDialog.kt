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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun FeatureDisabledWithProxyDialogContent(
    dialogState: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onActionButtonClicked: (() -> Unit)? = null
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.not_supported_dialog_title),
            text = if (state.teamUrl.isEmpty()) {
                stringResource(state.description)
            } else {
                stringResource(state.description, state.teamUrl)
            },

            onDismiss = dialogState::dismiss,
            optionButton2Properties =
            if (onActionButtonClicked != null) {
                WireDialogButtonProperties(
                    onClick = onActionButtonClicked,
                    text = stringResource(id = R.string.to_team_management_action),
                    type = WireDialogButtonType.Primary
                )
            } else {
                null
            },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = android.R.string.ok),
                type = WireDialogButtonType.Primary,
            ),
            buttonsHorizontalAlignment = false
        )
    }
}
