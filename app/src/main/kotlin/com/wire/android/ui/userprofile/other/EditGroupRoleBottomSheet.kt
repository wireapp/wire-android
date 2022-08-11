package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.RichMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.kalium.logic.data.conversation.Member

@Composable
fun EditGroupRoleBottomSheet(
    groupState: OtherUserProfileGroupState,
    changeMemberRole: (Member.Role) -> Unit,
    closeChangeRoleBottomSheet: () -> Unit
) {
    MenuModalSheetContent(
        headerTitle = stringResource(R.string.user_profile_role_in_group, groupState.groupName),
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
    RichMenuBottomSheetItem(
        title = role.name.asString(),
        onItemClick = Clickable { onRoleClicked(role).also { closeChangeRoleBottomSheet() } },
        state = if (currentRole == role) RichMenuItemState.SELECTED else RichMenuItemState.DEFAULT
    )
}
