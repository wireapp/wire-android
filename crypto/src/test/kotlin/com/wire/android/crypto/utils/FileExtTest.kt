package com.wire.android.crypto.utils

import com.wire.android.crypto.UnitTest
import org.amshove.kluent.`should be equal to`
import org.junit.Test
import java.io.File

class FileExtTest : UnitTest() {

    @Test
    fun `given a file, when adding a string to it, then it should create a child file`() {
        val rootFile = File("ABC")
        val childSubPath = "test${File.separator}123"

        val childFile = rootFile + childSubPath

        childFile `should be equal to` File(rootFile, childSubPath)
    }
}
