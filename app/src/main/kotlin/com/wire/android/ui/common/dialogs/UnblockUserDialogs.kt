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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.user.UserId

@Composable
fun UnblockUserDialogContent(
    dialogState: VisibilityState<UnblockUserDialogState>,
    onUnblock: (UnblockUserDialogState) -> Unit = { }
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(id = R.string.unblock_user_dialog_title),
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.unblock_user_dialog_body,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                state.userName
            ),
            onDismiss = dialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onUnblock(state) },
                text = stringResource(id = R.string.unblock_user_dialog_confirm_button),
                type = WireDialogButtonType.Primary,
                state = if (state.loading) WireButtonState.Disabled else WireButtonState.Default,
                loading = state.loading
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = android.R.string.cancel),
                type = WireDialogButtonType.Secondary,
            )
        )
    }
}
