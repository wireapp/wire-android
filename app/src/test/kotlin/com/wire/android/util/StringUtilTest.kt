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

import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun givenFilenameWithExtension_whenCalled_thenShouldInsertStringBeforeExtension() {
        val originalFilename = "document.pdf"
        val textToInsert = "new"

        val result = originalFilename.addBeforeExtension(textToInsert)

        assertEquals("document_new.pdf", result)
    }

    @Test
    fun givenFilenameWithoutExtension_whenCalled_thenShouldAppendString() {
        val originalFilename = "myfile"
        val textToInsert = "_version2"

        val result = originalFilename.addBeforeExtension(textToInsert)

        assertEquals("myfile_version2", result)
    }
    
    @Test
    fun givenFilenameWithMultipleDots_whenCalled_thenShouldInsertBeforeLastDot() {
        val originalFilename = "archive.2024.tar.gz"
        val textToInsert = "backup"

        val result = originalFilename.addBeforeExtension(textToInsert)

        assertEquals("archive.2024_backup.tar.gz", result)
    }

    @Suppress("LongMethod")
    @Test
    fun givenDifferentMarkdownsWithOnlyWhitespaces_whenCheckingIfNotBlank_thenReturnProperValues() {

        fun testIsNotMarkdownBlank(currentInput: String, expected: Boolean) {
            val result = currentInput.isNotMarkdownBlank()
            val errorMessage = "Expected $expected for input \"${currentInput}\".isNotMarkdownBlank() but got $result"
            assertEquals(expected, result, errorMessage)
        }

        testIsNotMarkdownBlank(" ", false)
        testIsNotMarkdownBlank("   ", false)
        testIsNotMarkdownBlank("\n", false)
        testIsNotMarkdownBlank("a", true)
        testIsNotMarkdownBlank(" a ", true)

        testIsNotMarkdownBlank("*", false) // empty bullet
        testIsNotMarkdownBlank("* ", false) // empty bullet
        testIsNotMarkdownBlank("*a", true) // just a text with one asterisk and "a"
        testIsNotMarkdownBlank("* a", true) // bullet with "a"
        testIsNotMarkdownBlank("* *", false) // two empty bullets
        testIsNotMarkdownBlank("* * a", true) // two bullets with "a"
        testIsNotMarkdownBlank("* * *", true) // thematic break
        testIsNotMarkdownBlank("* * * a", true) // three bullets with "a"
        testIsNotMarkdownBlank("**", true) // just a text with two asterisks
        testIsNotMarkdownBlank("** **", true) // thematic break
        testIsNotMarkdownBlank("**a**", true) // bold "a" text
        testIsNotMarkdownBlank("** a **", true) // bold " a " text
        testIsNotMarkdownBlank("**** ****", true) // thematic break
        testIsNotMarkdownBlank("****a****", true) // bold "a" text
        testIsNotMarkdownBlank("**\n**", true) // just two asterisks and another two asterisks in new line

        testIsNotMarkdownBlank("_", true) // just a text with one underscore
        testIsNotMarkdownBlank("__", true) // just a text with two underscores
        testIsNotMarkdownBlank("_ _", true) // just a text with two underscores and space between
        testIsNotMarkdownBlank("_a_", true) // italic "a" text
        testIsNotMarkdownBlank("_ a _", true) // italic " a " text
        testIsNotMarkdownBlank("__ __", true) // thematic break
        testIsNotMarkdownBlank("__a__", true) // bold "a" text
        testIsNotMarkdownBlank("_\n_", true) // just underline and another underline in new line

        testIsNotMarkdownBlank("#", false) // empty header
        testIsNotMarkdownBlank("##", false) // empty header
        testIsNotMarkdownBlank("#a", true) // just a text with one hash and "a"
        testIsNotMarkdownBlank("##a", true) // just a text with two hashes and "a"
        testIsNotMarkdownBlank("# ", false) // empty header
        testIsNotMarkdownBlank("# a", true) // header "a" text
        testIsNotMarkdownBlank("## a", true) // header "a" text

        testIsNotMarkdownBlank("#_a_", true) // just a text with one hash and italic "a"
        testIsNotMarkdownBlank("# _a_", true) // header italic "a" text
        testIsNotMarkdownBlank("#__", true) // just a text with one hash and two underscores
        testIsNotMarkdownBlank("#_ _", true) // just a text with one hash, two underscores and space between
        testIsNotMarkdownBlank("# __", true) // header with two underscores
        testIsNotMarkdownBlank("# _ _", true) // header with two underscores and space between

        testIsNotMarkdownBlank("_#_", true) // italic hash
        testIsNotMarkdownBlank("_# _", true) // just a text with underscores and hash with space between
        testIsNotMarkdownBlank("_#a_", true) // italic text with hash and "a"
        testIsNotMarkdownBlank("_# a_", true) // italic text with underscores and hash, space and "a" between

        testIsNotMarkdownBlank("#**a**", true) // just a text with one hash and bold "a"
        testIsNotMarkdownBlank("# **a**", true) // header bold "a" text
        testIsNotMarkdownBlank("#****", true) // just a text with one hash and four asterisks
        testIsNotMarkdownBlank("#** **", true) // just a text with one hash, four asterisks and space between
        testIsNotMarkdownBlank("# ** **", true) // header with four asterisks and space between

        testIsNotMarkdownBlank("**#**", true) // bold hash
        testIsNotMarkdownBlank("**# **", true) // just a text with asterisks and hash with space between
        testIsNotMarkdownBlank("**#a**", true) // bold text with hash and "a"
        testIsNotMarkdownBlank("**# a**", true) // bold text with underscores and hash, space and "a" between

        testIsNotMarkdownBlank("_****_", true) // italic four asterisks
        testIsNotMarkdownBlank("_** **_", true) // italic four asterisks with space between
        testIsNotMarkdownBlank("_**a**_", true) // bold italic "a" text
        testIsNotMarkdownBlank("**__**", true) // bold two underscores
        testIsNotMarkdownBlank("**_ _**", true) // bold two underscores with space between
        testIsNotMarkdownBlank("**_a_**", true) // bold italic "a" text
    }
}
