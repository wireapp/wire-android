package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions

@Composable
fun ContactSearchResultItem(
    //TODO : this will need refactor we are not using avatarUrl
    avatarUrl: String = "",
    userStatus: UserStatus,
    name: String,
    label: String,
    searchQuery: String,
    source: Source,
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                if (source is Source.Internal) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Spacer(Modifier.width(dimensions().spacing4x))
                }
                UserProfileAvatar(
                    status = userStatus
                )
            }

        },
        title = {
            HighLightName(
                name = name,
                searchQuery = searchQuery
            )
        },
        subTitle = {
            HighLightSubTitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = {
            if (source is Source.Internal) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(end = 8.dp)
                ) {
                    ArrowRightIcon(Modifier.align(Alignment.TopEnd))
                }
            } else {
                AddContactButton({ })
            }
        },
        onRowItemClicked = onRowItemClicked,
        onRowItemLongClicked = onRowItemLongClicked,
        modifier = modifier
    )

}

sealed class Source {
    object External : Source()
    object Internal : Source()
}
