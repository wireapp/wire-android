package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversationslist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationslist.model.EventType


@Composable
fun ContactSearchResultItem(
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
            UserProfileAvatar(
                avatarUrl = avatarUrl,
                status = userStatus
            )
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
                if (source.eventType != null) {
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(end = 8.dp)
                    ) {
                        EventBadgeFactory(eventType = source.eventType, modifier = Modifier.align(Alignment.TopEnd))
                    }
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
    data class Internal(val eventType: EventType?) : Source()
}
