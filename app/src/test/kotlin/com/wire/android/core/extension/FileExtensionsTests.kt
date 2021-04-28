package com.wire.android.core.extension

import com.wire.android.UnitTest
import com.wire.android.core.extension.plus
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.io.File

class FileExtensionsTests : UnitTest() {

    @Test
    fun `given a file, when adding a string to it, then it should create a child file`() {
        val rootFile = File("ABC")
        val childSubPath = "test${File.separator}123"

        val childFile = rootFile + childSubPath

        childFile shouldBeEqualTo File(rootFile, childSubPath)
    }
}
