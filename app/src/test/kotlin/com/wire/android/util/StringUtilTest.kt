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
 */
package com.wire.android.util

import org.junit.jupiter.api.Test

class StringUtilTest {

    @Test
    fun givenString_whenToTitleCase_thenReturnsTitleCase() {
        val input = "tHIS is a teSt"
        val expected = "This Is A Test"
        val actual = input.toTitleCase()
        assert(expected == actual)
    }

    @Test
    fun givenStringInLanguageWithNoUpperCase_whenToTitleCase_thenNothingChanges() {
        val input = "هذا اختبار"
        val expected = input
        val actual = input.toTitleCase()
        assert(expected == actual)
    }

    @Test
    fun givenString_whenNormalizingAsFileName_thenAllSlashesAreRemoved() {
        val input = "this/is/a/test"
        val expected = "thisisatest"
        val actual = input.normalizeFileName()
        assert(expected == actual)
    }
}
