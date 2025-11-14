/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.common.upgradetoapps

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.card.WireOutlinedCard
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun UpgradeToGetAppsBanner(
    modifier: Modifier = Modifier
) {
    WireOutlinedCard(
        title = stringResource(R.string.apps_upgrade_teams_for_apps_banner_title),
        textContent = stringResource(R.string.apps_upgrade_teams_for_apps_banner_content),
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = null,
                tint = colorsScheme().onBackground
            )
        },
        modifier = modifier.padding(
            start = MaterialTheme.wireDimensions.spacing8x,
            end = MaterialTheme.wireDimensions.spacing8x,
            top = MaterialTheme.wireDimensions.spacing8x,
            bottom = MaterialTheme.wireDimensions.spacing16x,
        )
    )
}
