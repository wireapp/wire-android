package com.wire.android.core.crypto.utils

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.io.File

class FileExtTest : UnitTest() {

    @Test
    fun `given a file, when adding a string to it, then it should create a child file`() {
        val rootFile = File("ABC")
        val childSubPath = "test${File.separator}123"

        val childFile = rootFile + childSubPath

        childFile shouldBeEqualTo File(rootFile, childSubPath)
    }
}
