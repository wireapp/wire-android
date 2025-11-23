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

package com.wire.android.ui.common.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun SecurityClassificationBannerForConversation(
    conversationId: ConversationId,
    modifier: Modifier = Modifier,
    viewModel: SecurityClassificationViewModel =
        hiltViewModelScoped<SecurityClassificationViewModelImpl, SecurityClassificationViewModel, SecurityClassificationViewModelImpl.Factory, SecurityClassificationArgs>(
            SecurityClassificationArgs.Conversation(id = conversationId)
        )
) {
    SecurityClassificationBanner(
        state = viewModel.state(),
        modifier = modifier
    )
}

@Composable
fun SecurityClassificationBannerForUser(
    userId: UserId,
    modifier: Modifier = Modifier,
    viewModel: SecurityClassificationViewModel =
        hiltViewModelScoped<SecurityClassificationViewModelImpl, SecurityClassificationViewModel, SecurityClassificationViewModelImpl.Factory, SecurityClassificationArgs>(
            SecurityClassificationArgs.User(id = userId)
        )
) {
    SecurityClassificationBanner(
        state = viewModel.state(),
        modifier = modifier
    )
}

@Composable
private fun SecurityClassificationBanner(
    state: SecurityClassificationType,
    modifier: Modifier = Modifier
) {
    if (state != SecurityClassificationType.NONE) {
        Column(modifier = modifier) {
            HorizontalDivider(color = getDividerColorFor(state))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(getBackgroundColorFor(state))
                    .height(dimensions().spacing24x)
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = getIconFor(state),
                    tint = getColorTextFor(state),
                    contentDescription = getTextFor(state),
                    modifier = Modifier.padding(end = dimensions().spacing8x)
                )
                Text(
                    text = getTextFor(state),
                    color = getColorTextFor(state),
                    style = MaterialTheme.wireTypography.label03
                )
            }
            HorizontalDivider(color = getDividerColorFor(state))
        }
    }
}

@Composable
private fun getTextFor(securityClassificationType: SecurityClassificationType): String {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        stringResource(id = R.string.conversation_details_is_classified)
    } else {
        stringResource(id = R.string.conversation_details_is_not_classified)
    }
}

@Composable
private fun getBackgroundColorFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().positiveVariant
    } else {
        colorsScheme().error
    }
}

@Composable
private fun getColorTextFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().onPositiveVariant
    } else {
        colorsScheme().onError
    }
}

@Composable
private fun getDividerColorFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().onPositiveVariant
    } else {
        colorsScheme().onError
    }
}

@Composable
private fun getIconFor(securityClassificationType: SecurityClassificationType): Painter {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        painterResource(id = R.drawable.ic_check_tick)
    } else {
        painterResource(id = R.drawable.ic_info)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewClassifiedIndicator() {
    WireTheme {
        Surface {
            Column(modifier = Modifier.fillMaxWidth()) {
                SecurityClassificationBanner(
                    state = SecurityClassificationType.NONE
                )
                HorizontalDivider()
                SecurityClassificationBanner(
                    state = SecurityClassificationType.NOT_CLASSIFIED
                )
                HorizontalDivider()
                SecurityClassificationBanner(
                    state = SecurityClassificationType.CLASSIFIED
                )
            }
        }
    }
}
