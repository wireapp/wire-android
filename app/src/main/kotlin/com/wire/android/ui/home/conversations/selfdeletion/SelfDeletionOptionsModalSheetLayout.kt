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
package com.wire.android.ui.home.conversations.selfdeletion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.message.SelfDeletionTimer

@Composable
fun SelfDeletionOptionsModalSheetLayout(
    sheetState: WireModalSheetState<SelfDeletionTimer>,
    onNewSelfDeletingMessagesStatus: (SelfDeletionTimer) -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = { currentlySelected ->
            WireMenuModalSheetContent(
                header = MenuModalSheetHeader.Visible(title = stringResource(R.string.automatically_delete_message_after)),
                menuItems = selfDeletionMenuItems(
                    currentlySelected = currentlySelected.duration.toSelfDeletionDuration(),
                    onSelfDeletionDurationChanged = remember {
                        { newTimer ->
                            sheetState.hide {
                                onNewSelfDeletingMessagesStatus(SelfDeletionTimer.Enabled(newTimer.value))
                            }
                        }
                    }
                )
            )
        }
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewSelfDeletionOptionsModalSheetLayout() = WireTheme {
    SelfDeletionOptionsModalSheetLayout(
        sheetState = rememberWireModalSheetState(initialValue = WireSheetValue.Expanded(SelfDeletionTimer.Disabled)),
        onNewSelfDeletingMessagesStatus = {}
    )
}
