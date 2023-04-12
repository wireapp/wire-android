package com.wire.android.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon

@Composable
fun MessageDetailsMenuOption(
    onMessageDetailsClick: () -> Unit
) {
    MenuBottomSheetItem(
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_info,
                contentDescription = stringResource(R.string.content_description_open_message_details),
            )
        },
        title = stringResource(R.string.label_message_details),
        onItemClick = onMessageDetailsClick
    )
}
