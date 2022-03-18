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
