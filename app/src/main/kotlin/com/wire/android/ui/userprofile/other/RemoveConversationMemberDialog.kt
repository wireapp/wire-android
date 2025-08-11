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

package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState

@Composable
internal fun RemoveConversationMemberDialog(
    dialogState: VisibilityState<RemoveConversationMemberState>,
    onRemoveConversationMember: (RemoveConversationMemberState) -> Unit,
) {

    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.dialog_remove_conversation_member_title),
            text = stringResource(
                R.string.dialog_remove_conversation_member_description,
                state.fullName,
                state.userName
            ),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onRemoveConversationMember(state) },
                text = stringResource(id = R.string.label_remove),
                type = WireDialogButtonType.Primary,
                state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
                loading = state.loading
            )
        )
    }

}
