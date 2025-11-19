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
package com.wire.android.ui.home.conversations.search.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun EmptySearchAppsContent(
    isSelfATeamAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val (title, description) = if (isSelfATeamAdmin) {
        Pair(
            stringResource(R.string.search_results_apps_empty_title),
            stringResource(R.string.search_results_apps_empty_description_team_admin)
        )
    } else {
        Pair(
            stringResource(R.string.search_results_apps_empty_title),
            stringResource(R.string.search_results_apps_empty_description_non_team_admin)
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensions().spacing16x))
        Text(
            text = description,
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center
        )
    }
}
