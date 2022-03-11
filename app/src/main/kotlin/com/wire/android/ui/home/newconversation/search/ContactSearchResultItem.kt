package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.newconversation.contacts.Contact

@Composable
fun ContactSearchResultItem(
    contactSearchResult: Contact,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    with(contactSearchResult) {
        RowItemTemplate(
            leadingIcon = {
                UserProfileAvatar(
                    avatarUrl = "",
                    status = UserStatus.AVAILABLE
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
            eventType = eventType,
            onRowItemClicked = {},
            onRowItemLongClicked = {},
            modifier = modifier
        )
    }
}
