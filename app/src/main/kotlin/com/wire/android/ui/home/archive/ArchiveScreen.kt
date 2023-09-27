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

package com.wire.android.ui.home.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@HomeNavGraph
@Destination
@Composable
fun ArchiveScreen() {
    ArchivedConversationsEmptyStateScreen()
}

@Composable
fun ArchivedConversationsEmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing24x,
            ),
            text = stringResource(R.string.archive_screen_empty_state_title),
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing8x,
                start = dimensions().spacing40x,
                end = dimensions().spacing40x
            ),
            text = stringResource(R.string.archive_screen_empty_state_description),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewArchiveScreen() {
    ArchiveScreen()
}
