package com.wire.android.util

import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileUtilTest {

    @TempDir
    val tempDir = File("temp-dir")

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
}
