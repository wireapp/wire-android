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
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun SecurityClassificationBanner(
    securityClassificationType: SecurityClassificationType,
    modifier: Modifier = Modifier
) {
    if (securityClassificationType != SecurityClassificationType.NONE) {
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(getBackgroundColorFor(securityClassificationType))
                .height(dimensions().spacing24x)
                .fillMaxWidth()
        ) {
            Text(
                text = getTextFor(securityClassificationType),
                color = getColorTextFor(securityClassificationType),
                style = MaterialTheme.wireTypography.label03
            )
        }
        Divider()
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
        colorsScheme().surface
    } else {
        colorsScheme().onSurface
    }
}

@Composable
private fun getColorTextFor(securityClassificationType: SecurityClassificationType): Color {
    return if (securityClassificationType == SecurityClassificationType.CLASSIFIED) {
        colorsScheme().onSurface
    } else {
        colorsScheme().surface
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewClassifiedIndicator() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SecurityClassificationBanner(securityClassificationType = SecurityClassificationType.CLASSIFIED)
        Divider()
        SecurityClassificationBanner(securityClassificationType = SecurityClassificationType.NOT_CLASSIFIED)
    }
}
