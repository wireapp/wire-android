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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.QueryMatchExtractor

@Composable
fun HighlightSubtitle(
    subTitle: String,
    modifier: Modifier = Modifier,
    searchQuery: String = String.EMPTY,
    prefix: String = "@"
) {
    if (subTitle.isBlank()) {
        return
    }

    val subtitleWithPrefix = "$prefix$subTitle"

    val highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
        matchText = searchQuery,
        text = subtitleWithPrefix
    )

    if (searchQuery != String.EMPTY && highlightIndexes.isNotEmpty()) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.wireColorScheme.secondaryText,
                        fontWeight = MaterialTheme.wireTypography.subline01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.subline01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.subline01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.subline01.fontStyle
                    )
                ) {
                    append(subtitleWithPrefix)
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
            text = subtitleWithPrefix,
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}
