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

import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileUtilTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `given file does not exist when finding first unique name in directory then return this name`() {
        val desired = "abc.jpg"
        val expected = "abc.jpg"

        val result = findFirstUniqueName(tempDir, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file already exists when finding unique name in directory then return next available name`() {
        File(tempDir, "abc.jpg").createNewFile()
        val desired = "abc.jpg"
        val expected = "abc (1).jpg"

        val result = findFirstUniqueName(tempDir, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file and its copies already exist when finding unique name in directory then return next available name`() {
        File(tempDir, "abc.jpg").createNewFile()
        File(tempDir, "abc (1).jpg").createNewFile()
        File(tempDir, "abc (2).jpg").createNewFile()
        val desired = "abc.jpg"
        val expected = "abc (3).jpg"

        val result = findFirstUniqueName(tempDir, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file with invalid filename when finding unique name in directory then return name without disallowed characters`() {
        val desired = "\u0020ab\u0008cd\u0000ef\u001fgh\u007Fij*kl/mn:op<qr>st?uv\\wx|yz.jpg"
        val expected = "\u0020ab_cd_ef_gh_ij_kl_mn_op_qr_st_uv_wx_yz.jpg"

        val result = desired.sanitizeFilename()
        assertEquals(expected, result)
    }
}
