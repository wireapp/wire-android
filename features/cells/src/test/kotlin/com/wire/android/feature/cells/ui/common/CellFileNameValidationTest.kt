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
package com.wire.android.feature.cells.ui.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CellFileNameValidationTest {

    @Test
    fun `given file name too long, when validating, then correct error returned`() = runTest {
        val filename = "Long name that exceeds the limit of sixty four characters will return error."
        val result = filename.validateFileName()
        assertEquals(FileNameError.NameExceedLimit, result)
    }

    @Test
    fun `given file name empty, when validating, then correct error returned`() = runTest {
        val filename = ""
        val result = filename.validateFileName()
        assertEquals(FileNameError.NameEmpty, result)
    }

    @Test
    fun `given file name contains slash, when validating, then correct error returned`() = runTest {
        val filename = "File/Name"
        val result = filename.validateFileName()
        assertEquals(FileNameError.InvalidName, result)
    }

    @Test
    fun `given file name contains backslash, when validating, then correct error returned`() = runTest {
        val filename = "Fi\\leName"
        val result = filename.validateFileName()
        assertEquals(FileNameError.InvalidName, result)
    }

    @Test
    fun `given file name contains double quotes, when validating, then correct error returned`() = runTest {
        val filename = "Fi\"leName"
        val result = filename.validateFileName()
        assertEquals(FileNameError.InvalidName, result)
    }

    @Test
    fun `given file name starts with dot, when validating, then correct error returned`() = runTest {
        val filename = ".filename"
        val result = filename.validateFileName()
        assertEquals(FileNameError.InvalidName, result)
    }

    @Test
    fun `given file name only has dot, when validating, then correct error returned`() = runTest {
        val filename = "."
        val result = filename.validateFileName()
        assertEquals(FileNameError.InvalidName, result)
    }

    @Test
    fun `given file name is valid, when validating, then no error returned`() = runTest {
        val filename = "valid file name"
        val result = filename.validateFileName()
        assertEquals(null, result)
    }
}
