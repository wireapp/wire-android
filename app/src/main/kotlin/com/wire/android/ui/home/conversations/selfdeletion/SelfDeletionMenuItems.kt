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
 */
package com.wire.android.ui.home.conversations.selfdeletion

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.home.conversations.edit.OnComplete
import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import com.wire.android.ui.theme.wireTypography

@Composable
fun SelfDeletionMenuItems(
    currentlySelected: SelfDeletionDuration,
    hideEditMessageMenu: (OnComplete) -> Unit,
    onSelfDeletionDurationChanged: (SelfDeletionDuration) -> Unit,
): List<@Composable () -> Unit> {

    val onSelfDeletionDurationSelected: (SelfDeletionDuration) -> Unit = { selfDeleteDuration ->
        hideEditMessageMenu {
            onSelfDeletionDurationChanged(selfDeleteDuration)
        }
    }

    return buildList {
        add {
            val duration = SelfDeletionDuration.None
            SelfDeletionDurationMenuItem(
                duration = duration,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.TenSeconds
            SelfDeletionDurationMenuItem(
                duration = duration,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.FiveMinutes
            SelfDeletionDurationMenuItem(
                duration = duration,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.OneHour
            SelfDeletionDurationMenuItem(
                duration = duration,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.OneDay
            SelfDeletionDurationMenuItem(
                duration = SelfDeletionDuration.OneDay,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.OneWeek
            SelfDeletionDurationMenuItem(
                duration = SelfDeletionDuration.OneWeek,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
        add {
            val duration = SelfDeletionDuration.FourWeeks
            SelfDeletionDurationMenuItem(
                duration = SelfDeletionDuration.FourWeeks,
                isSelected = currentlySelected == duration,
                onSelfDeletionDurationSelected = onSelfDeletionDurationSelected
            )
        }
    }
}

@Composable
private fun SelfDeletionDurationMenuItem(
    duration: SelfDeletionDuration,
    isSelected: Boolean,
    onSelfDeletionDurationSelected: (SelfDeletionDuration) -> Unit
) {
    with(duration) {
        SelectableMenuBottomSheetItem(
            title = label,
            titleStyleUnselected = MaterialTheme.wireTypography.body01,
            titleStyleSelected = MaterialTheme.wireTypography.body01,
            onItemClick = Clickable { onSelfDeletionDurationSelected(duration) },
            state = if (isSelected) RichMenuItemState.SELECTED
            else RichMenuItemState.DEFAULT
        )
    }
}
