package com.wire.android.core.io

import com.wire.android.AndroidTest
import com.wire.android.core.exception.FileDoesNotExist
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEndWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

//FIXME: Robolectric doesn't support legacy mode after P
@Config(minSdk = 24, maxSdk = 30)
class FileSystemTest : AndroidTest() {

    private lateinit var fileSystem: FileSystem

    @Before
    fun setUp() {
        fileSystem = FileSystem(context())
    }

    @After
    fun tearDown() {
        File(TEST_DIRECTORY).deleteRecursively()
        context().filesDir.deleteRecursively()
    }

    @Test
    fun `given createInternalFile is called, when file is successfully created in internal storage, then propagates success`() {
        val result = fileSystem.createInternalFile(TEST_PATH)

        result shouldSucceed {
            it.absolutePath shouldEndWith TEST_PATH
            it.exists() shouldBeEqualTo true
        }
    }

    @Test
    fun `given internalFile is called, when file does not exist in internal storage, then propagates FileDoesNotExist failure`() {
        val result = fileSystem.internalFile(TEST_PATH)

        result shouldFail { it shouldBeEqualTo FileDoesNotExist }
    }

    @Test
    fun `given internalFile is called, when file exists in internal storage, then propagates success`() {
        val file = createFile(File(context().filesDir, TEST_PATH))

        file.exists() shouldBeEqualTo true

        val result = fileSystem.internalFile(TEST_PATH)

        result shouldSucceed { it.absolutePath shouldBeEqualTo file.absolutePath }
    }

    @Test
    fun `given writeToFile is called, when file does not exist, then propagates FileDoesNotExist failure`() {
        val result = fileSystem.writeToFile(File(TEST_DIRECTORY, TEST_PATH), TEST_INPUT_STREAM)

        result shouldFail { it shouldBeEqualTo FileDoesNotExist }
    }

    @Test
    fun `given writeToFile is called for an existing file, when stream is successfully written, then propagates success`() {
        val file = createFile(File(TEST_DIRECTORY, TEST_PATH))

        file.exists() shouldBeEqualTo true

        val result = fileSystem.writeToFile(file, TEST_INPUT_STREAM)

        result shouldSucceed  {
            it.readBytes().decodeToString() shouldBeEqualTo TEST_FILE_CONTENT
        }
    }

    private fun createFile(file: File): File {
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }

    companion object {
        private const val TEST_DIRECTORY = "file_system_test_files"
        private const val TEST_PATH = "file_system_test/test_file.txt"

        private const val TEST_FILE_CONTENT = "some content"
        private val TEST_INPUT_STREAM = ByteArrayInputStream(TEST_FILE_CONTENT.toByteArray())
    }
}
