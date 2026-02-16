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
package com.wire.android.ui.common.rowitem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun EmptyListContent(
    title: String?,
    text: String,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = { EmptyListArrowFooter() },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                dimensions().spacing40x
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        title?.let { title ->
            Text(
                modifier = Modifier.padding(
                    bottom = dimensions().spacing16x,
                    top = dimensions().spacing100x
                ),
                text = title,
                style = MaterialTheme.wireTypography.title01,
                color = MaterialTheme.wireColorScheme.onSurface,
            )
        }
        Text(
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
            text = text,
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        footer()
    }
}

@Composable
fun EmptyListArrowFooter(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing100x),
        painter = painterResource(
            id = R.drawable.ic_empty_conversation_arrow
        ),
        contentDescription = null
    )
}
