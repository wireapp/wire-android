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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun BlockedUserComposerInput(securityClassificationType: SecurityClassificationType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().backgroundVariant)
            .padding(dimensions().spacing16x)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = "",
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .size(dimensions().spacing12x)
        )
        Text(
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.label_system_message_blocked_user,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().secondaryText,
                colorsScheme().onBackground,
                stringResource(id = R.string.member_name_you_label_titlecase)
            ),
            style = MaterialTheme.wireTypography.body01,
            maxLines = 1,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .padding(start = dimensions().spacing16x)
        )
    }
    MessageComposerClassifiedBanner(securityClassificationType = securityClassificationType)
}
