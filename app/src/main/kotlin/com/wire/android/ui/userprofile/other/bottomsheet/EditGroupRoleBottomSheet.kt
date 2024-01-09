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

package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.other.OtherUserProfileGroupState
import com.wire.android.ui.userprofile.other.name
import com.wire.kalium.logic.data.conversation.Conversation.Member

@Composable
fun EditGroupRoleBottomSheet(
    groupState: OtherUserProfileGroupState,
    changeMemberRole: (Member.Role) -> Unit,
    closeChangeRoleBottomSheet: () -> Unit
) {
    MenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(title = stringResource(R.string.user_profile_role_in_group, groupState.groupName)),
        menuItems = listOf(
            { EditGroupRoleItem(Member.Role.Admin, groupState.role, changeMemberRole, closeChangeRoleBottomSheet) },
            { EditGroupRoleItem(Member.Role.Member, groupState.role, changeMemberRole, closeChangeRoleBottomSheet) }
        ),
    )
}

@Composable
private fun EditGroupRoleItem(
    role: Member.Role,
    currentRole: Member.Role,
    onRoleClicked: (Member.Role) -> Unit,
    closeChangeRoleBottomSheet: () -> Unit
) {
    SelectableMenuBottomSheetItem(
        title = role.name.asString(),
        titleStyleUnselected = MaterialTheme.wireTypography.body01,
        titleStyleSelected = MaterialTheme.wireTypography.body02,
        onItemClick = Clickable { onRoleClicked(role).also { closeChangeRoleBottomSheet() } },
        state = if (currentRole == role) RichMenuItemState.SELECTED else RichMenuItemState.DEFAULT
    )
}
