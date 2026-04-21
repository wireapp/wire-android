/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */
package com.wire.android.ui.userprofile.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun ServiceDetailsNotFoundScreen(
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing16x
            )
    ) {
        UserProfileAvatar(
            size = dimensions().avatarDefaultMediumSize,
            temporaryUserBorderWidth = dimensions().avatarTemporaryUserBorderWidth,
            avatarData = UserAvatarData(
                asset = null,
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Service
            ),
            clickable = remember { Clickable(enabled = false) },
            type = UserProfileAvatarType.WithoutIndicators,
        )

        Spacer(modifier = Modifier.width(dimensions().spacing12x))

        Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
            ) {
                Text(
                    text = stringResource(id = R.string.service_no_information_available_title),
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                    style = MaterialTheme.wireTypography.title02,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = stringResource(id = R.string.service_no_information_available_subtitle),
                maxLines = 2,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
