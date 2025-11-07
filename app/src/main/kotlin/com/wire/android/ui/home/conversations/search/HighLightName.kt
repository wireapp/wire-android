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

package com.wire.android.ui.home.conversations.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.QueryMatchExtractor
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun HighlightName(
    name: String,
    searchQuery: String,
    modifier: Modifier = Modifier
) {

    val highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
        matchText = searchQuery,
        text = name
    )

    if (searchQuery != String.EMPTY && highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = MaterialTheme.wireTypography.title02.fontWeight,
                        fontSize = MaterialTheme.wireTypography.title02.fontSize,
                        fontFamily = MaterialTheme.wireTypography.title02.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.title02.fontStyle
                    )
                ) {
                    append(name)
                }

                highlightIndexes
                    .forEach { highLightIndex ->
                        if (highLightIndex.endIndex <= this.length) {
                            addStyle(
                                style = SpanStyle(
                                    background = MaterialTheme.wireColorScheme.highlight,
                                    color = MaterialTheme.wireColorScheme.onHighlight,
                                ),
                                start = highLightIndex.startIndex,
                                end = highLightIndex.endIndex
                            )
                        }
                    }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    } else {
        Text(
            text = name,
            style = MaterialTheme.wireTypography.title02.copy(
                color = if (name.isUnknownUser()) {
                    MaterialTheme.wireColorScheme.secondaryText
                } else {
                    MaterialTheme.wireTypography.title02.color
                }
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

@Composable
private fun String.isUnknownUser() = this == stringResource(id = R.string.username_unavailable_label)

@PreviewMultipleThemes
@Composable
fun PreviewHighlightName() {
    WireTheme {
        HighlightName(
            name = "John Doe",
            searchQuery = "John"
        )
    }
}
