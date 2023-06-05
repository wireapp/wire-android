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

package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun SecurityClassificationBanner(
    securityClassificationType: SecurityClassificationType,
    modifier: Modifier = Modifier
) {
    if (securityClassificationType != SecurityClassificationType.NONE) {
        Column {
            VerticalSpace.x8()
            Divider(color = getDividerColorFor(securityClassificationType))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .background(getBackgroundColorFor(securityClassificationType))
                    .height(dimensions().spacing24x)
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = getIconFor(securityClassificationType),
                    tint = getColorTextFor(securityClassificationType),
                    contentDescription = getTextFor(securityClassificationType),
                    modifier = Modifier.padding(end = dimensions().spacing8x)
                )
                Text(
                    text = getTextFor(securityClassificationType),
                    color = getColorTextFor(securityClassificationType),
                    style = MaterialTheme.wireTypography.label03
                )
            }
            Divider(color = getDividerColorFor(securityClassificationType))
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
        colorsScheme().classifiedBannerBackgroundColor
    } else {
        colorsScheme().unclassifiedBannerBackgroundColor
    }
}

@Composable
private fun getColorTextFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().classifiedBannerForegroundColor
    } else {
        colorsScheme().unclassifiedBannerForegroundColor
    }
}

@Composable
private fun getDividerColorFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().classifiedBannerForegroundColor
    } else {
        colorsScheme().unclassifiedBannerBackgroundColor
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
                SecurityClassificationBanner(securityClassificationType = SecurityClassificationType.CLASSIFIED)
                Divider()
                SecurityClassificationBanner(securityClassificationType = SecurityClassificationType.NOT_CLASSIFIED)
            }
        }
    }
}
