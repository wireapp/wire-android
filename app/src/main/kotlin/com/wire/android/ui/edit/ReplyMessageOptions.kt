package com.wire.android.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon

@Composable
fun ReplyMessageOptions(onReplyItemClick: () -> Unit) {
    MenuBottomSheetItem(
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_reply,
                contentDescription = stringResource(R.string.content_description_reply_to_messge),
            )
        },
        title = stringResource(R.string.notification_action_reply),
        onItemClick = onReplyItemClick
    )
}
