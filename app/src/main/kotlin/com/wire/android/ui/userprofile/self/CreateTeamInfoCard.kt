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

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.card.WireOutlinedCard
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CreateTeamInfoCard(
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireOutlinedCard(
        title = stringResource(R.string.user_profile_create_team_card),
        textContent = stringResource(R.string.user_profile_create_team_description_card),
        mainActionButtonText = stringResource(R.string.user_profile_create_team_card_button),
        onMainActionClick = onCreateAccount,
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = colorsScheme().onBackground
            )
        },
        modifier = modifier
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewCreateTeamInfoCard() = WireTheme {
    CreateTeamInfoCard({ })
}
