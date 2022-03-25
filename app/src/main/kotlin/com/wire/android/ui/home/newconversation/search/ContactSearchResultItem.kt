package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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

@Composable
fun ContactSearchResultItem(
    //TODO : this will need refactor we are not using avatarUrl
    avatarUrl: String = "",
    userStatus: UserStatus,
    name: String,
    label: String,
    searchQuery: String,
    searchSource: SearchSource,
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                if (searchSource == SearchSource.Internal) {
                    Checkbox(checked = false, onCheckedChange = {})
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
            if (searchSource == SearchSource.Internal) {
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

