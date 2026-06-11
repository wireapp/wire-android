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

package com.wire.android.util

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueryMatchExtractorTest {

    @Test
    fun `given empty text, when extracting indexes, then return empty list`() = runTest {
        val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "test", text = "")
        assertEquals(emptyList<MatchQueryResult>(), result)
    }

    @Test
    fun `given empty matchText, when extracting indexes, then return empty list`() = runTest {
        val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "", text = "This is a sample text")
        assertEquals(emptyList<MatchQueryResult>(), result)
    }

    @Test
    fun `given one occurrence of matchText, when extracting indexes, then return list with one match`() = runTest {
        val ten = 10
        val fourteen = 14

        val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "test", text = "This is a test")
        assertEquals(listOf(MatchQueryResult(ten, fourteen)), result)
    }

    @Test
    fun `given multiple occurrences of matchText, when extracting indexes, then return list with all matches`() = runTest {
        val zero = 0
        val four = 4
        val eight = 8
        val twelve = 12

        val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "test", text = "test or test")
        assertEquals(listOf(MatchQueryResult(zero, four), MatchQueryResult(eight, twelve)), result)
    }

    @Test
    fun `given overlapping occurrences of matchText, when extracting indexes, then return list with non-overlapping matches`() =
        runTest {
            val zero = 0
            val two = 2
            val four = 4

            val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "ab", text = "abab")
            assertEquals(listOf(MatchQueryResult(zero, two), MatchQueryResult(two, four)), result)
        }

    @Test
    fun `given matchText is case insensitive, when extracting indexes, then return correct matches`() = runTest {
        val ten = 10
        val fourteen = 14
        val fifteen = 15
        val nineteen = 19

        val result = QueryMatchExtractor.extractQueryMatchIndexes(matchText = "TeSt", text = "This is a test TEST")
        assertEquals(listOf(MatchQueryResult(ten, fourteen), MatchQueryResult(fifteen, nineteen)), result)
    }
}
