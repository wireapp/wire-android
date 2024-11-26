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
package com.wire.android.ui.userprofile.self

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CreateTeamInfoCard(
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorsScheme().createTeamInfoCardBackgroundColor
        ),
        border = BorderStroke(dimensions().spacing1x, colorsScheme().createTeamInfoCardBorderColor),
    ) {
        Row(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                top = dimensions().spacing8x,
                end = dimensions().spacing8x
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = colorsScheme().onBackground
            )
            Text(
                modifier = Modifier.padding(start = dimensions().spacing8x),
                text = stringResource(R.string.user_profile_create_team_card),
                style = MaterialTheme.wireTypography.label02,
                color = colorsScheme().onBackground
            )
        }
        Text(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                top = dimensions().spacing4x,
                end = dimensions().spacing8x
            ),
            text = stringResource(R.string.user_profile_create_team_description_card),
            style = MaterialTheme.wireTypography.subline01,
            color = colorsScheme().onBackground
        )
        WireSecondaryButton(
            modifier = Modifier
                .padding(dimensions().spacing8x)
                .height(dimensions().createTeamInfoCardButtonHeight),
            text = stringResource(R.string.user_profile_create_team_card_button),
            onClick = onCreateAccount,
            fillMaxWidth = false,
            minSize = dimensions().buttonSmallMinSize,
            minClickableSize = dimensions().buttonMinClickableSize,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCreateTeamInfoCard() {
    WireTheme {
        CreateTeamInfoCard({ })
    }
}
