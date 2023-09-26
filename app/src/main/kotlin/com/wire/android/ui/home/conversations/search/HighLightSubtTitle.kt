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

package com.wire.android.ui.home.conversations.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.MatchQueryResult
import com.wire.android.util.QueryMatchExtractor
import kotlinx.coroutines.launch

@Composable
fun HighlightSubtitle(
    subTitle: String,
    searchQuery: String = "",
    suffix: String = "@"
) {
    val scope = rememberCoroutineScope()
    var highlightIndexes by remember {
        mutableStateOf(emptyList<MatchQueryResult>())
    }

    val queryWithoutSuffix = searchQuery.removeQueryPrefix()

    SideEffect {
        scope.launch {
            highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
                matchText = queryWithoutSuffix,
                text = subTitle
            )
        }
    }

    if (queryWithoutSuffix != String.EMPTY && highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.wireColorScheme.secondaryText,
                        fontWeight = MaterialTheme.wireTypography.subline01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.subline01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.subline01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.subline01.fontStyle
                    )
                ) {
                    append("$suffix$subTitle")
                }

                highlightIndexes
                    .forEach { highLightIndex ->
                        if (highLightIndex.endIndex <= this.length) {
                            addStyle(
                                style = SpanStyle(
                                    background = MaterialTheme.wireColorScheme.highLight.copy(alpha = 0.5f),
                                ),
                                start = highLightIndex.startIndex + suffix.length,
                                end = highLightIndex.endIndex + suffix.length
                            )
                        }
                    }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = "$suffix$subTitle",
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
