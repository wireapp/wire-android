package com.wire.android.ui.userprofile.self

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.self.model.OtherAccount

@Composable
internal fun OtherAccountsHeader() {
    SectionHeader(stringResource(id = R.string.user_profile_other_accs))
}

@Composable
internal fun OtherAccountItem(
    account: OtherAccount,
    clickable: Clickable = Clickable(enabled = true) {},
) {
    RowItemTemplate(
        leadingIcon = { UserProfileAvatar(account.avatarData) },
        titleStartPadding = dimensions().spacing0x,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = account.fullName,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    searchQuery = ""
                )
            }
        },
        subtitle = {
            val subTitle = buildString {
                append(account.handle ?: "")
                if (account.id.domain.isNotBlank()) {
                    append("@${account.id.domain}")
                }
            }
            HighlightSubtitle(subTitle = subTitle)
        },
        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = MaterialTheme.wireDimensions.spacing8x)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd), R.string.content_description_empty)
            }
        },
        clickable = clickable,
        modifier = Modifier.padding(start = dimensions().spacing8x)
    )
}
