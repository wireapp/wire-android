package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.Composable
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.newconversation.contacts.ExternalContact


@Composable
fun ExternalContactResultItem(
    externalContactSearchResult: ExternalContact,
    searchQuery: String
) {
    with(externalContactSearchResult) {
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
            actions = { AddContactButton({ }) },
            onRowItemClicked = {},
            onRowItemLongClicked = {},
        )
    }
}
