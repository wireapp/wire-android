/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.kalium.logic.data.conversation.Conversation

@Composable
fun UserLabel(
    searchQuery: String,
    userInfoLabel: UserInfoLabel,
    modifier: Modifier = Modifier
) {
    with(userInfoLabel) {
        ConversationTitle(
            name = if (unavailable) stringResource(id = R.string.username_unavailable_label) else labelName,
            isLegalHold = isLegalHold,
            modifier = modifier,
            badges = {
                if (membership.hasLabel()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    MembershipQualifierLabel(membership)
                }
                if (proteusVerificationStatus == Conversation.VerificationStatus.VERIFIED)
                    ProteusVerifiedIcon(contentDescriptionId = R.string.content_description_proteus_certificate_valid)
                if (mlsVerificationStatus == Conversation.VerificationStatus.VERIFIED)
                    MLSVerifiedIcon(contentDescriptionId = R.string.content_description_mls_certificate_valid)
            },
            searchQuery = searchQuery
        )
    }
}

data class UserInfoLabel(
    val labelName: String,
    val isLegalHold: Boolean,
    val membership: Membership,
    val unavailable: Boolean = false,
    val proteusVerificationStatus: Conversation.VerificationStatus? = null,
    val mlsVerificationStatus: Conversation.VerificationStatus? = null,
)
