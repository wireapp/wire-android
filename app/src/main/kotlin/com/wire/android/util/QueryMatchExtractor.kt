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

package com.wire.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QueryMatchExtractor {
    /**
     * extractHighLightIndexes is a recursive function returning a list of the start index and end index
     * of the [matchText] that is a text we want to search within the String provided as [text].
     * [resultMatches] contains a list of QueryResult with startIndex and endIndex if the match is found.
     * [startIndex] is a index from which we start the search
     */
    suspend fun extractQueryMatchIndexes(
        resultMatches: List<MatchQueryResult> = emptyList(),
        startIndex: Int = 0,
        matchText: String,
        text: String
    ): List<MatchQueryResult> =
        withContext(Dispatchers.Default) {
            if (matchText.isEmpty()) {
                return@withContext listOf()
            }
            val index = text.indexOf(matchText, startIndex = startIndex, ignoreCase = true)

            if (isIndexFound(index)) {
                extractQueryMatchIndexes(
                    resultMatches = resultMatches + MatchQueryResult(
                        startIndex = index,
                        endIndex = index + matchText.length
                    ),
                    // we are incrementing the startIndex by 1 for the next recursion
                    // to start looking for the match from the next index that we ended up
                    // finding the match for the matchText
                    startIndex = index + 1,
                    matchText = matchText,
                    text = text
                )
            } else {
                resultMatches
            }
        }

    private fun isIndexFound(index: Int): Boolean {
        return index != -1
    }
}

data class MatchQueryResult(val startIndex: Int, val endIndex: Int)
