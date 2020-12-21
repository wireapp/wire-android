package com.wire.android.shared.asset.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.io.FileSystem
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class AssetLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var assetDao: AssetDao

    @MockK
    private lateinit var fileSystem: FileSystem

    private lateinit var assetLocalDataSource: AssetLocalDataSource

    @Before
    fun setUp() {
        assetLocalDataSource = AssetLocalDataSource(assetDao, fileSystem)
    }

    @Test
    fun `given assetById is called, when dao call returns an asset, then propagates success`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldSucceed { it shouldBeEqualTo assetEntity }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given assetById is called, when dao call fails, then propagates failure`() {
        coEvery { assetDao.assetById(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail {}
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given createAsset is called with key and localDir, then calls assetDao to insert entity with correct params`() {
        coEvery { assetDao.insert(any()) } returns TEST_ID_LONG

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        val assetEntitySlot = slot<AssetEntity>()
        coVerify(exactly = 1) { assetDao.insert(capture(assetEntitySlot)) }
        assetEntitySlot.captured.downloadKey shouldBeEqualTo TEST_DOWNLOAD_KEY
    }

    @Test
    fun `given createAsset is called, when dao call returns an id of the inserted asset, then propagates success`() {
        coEvery { assetDao.insert(any()) } returns TEST_ID_LONG

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        result shouldSucceed { it shouldBeEqualTo TEST_ID }
    }

    @Test
    fun `given createAsset is called, when dao call fails, then propagates failure`() {
        coEvery { assetDao.insert(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        result shouldFail {}
    }

    @Test
    fun `given saveInternalAsset is called, when internal file cannot be created, then propagates failure`() {
        val failure = mockk<Failure>()
        every { fileSystem.createInternalFile(any()) } returns Either.Left(failure)

        val result = runBlocking { assetLocalDataSource.saveInternalAsset(TEST_ID, mockk()) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_ASSET_RELATIVE_PATH) }
    }

    @Test
    fun `given saveInternalAsset is called and internal file is created, when file cannot be written, then propagates failure`() {
        val file = mockk<File>()
        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        val failure = mockk<Failure>()
        every { fileSystem.writeToFile(file, any()) } returns Either.Left(failure)

        val inputStream = mockk<InputStream>()
        val result = runBlocking { assetLocalDataSource.saveInternalAsset(TEST_ID, inputStream) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_ASSET_RELATIVE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, inputStream) }
    }

    @Test
    fun `given saveInternalAsset is called and contents are written, when storage type cannot be updated, then propagates failure`() {
        val file = mockk<File>()
        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        every { fileSystem.writeToFile(file, any()) } returns Either.Right(file)
        coEvery { assetDao.updateStorageType(any(), any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.saveInternalAsset(TEST_ID, mockk()) }

        result shouldFail { }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_ASSET_RELATIVE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, any()) }
        coVerify(exactly = 1) { assetDao.updateStorageType(TEST_ID, STORAGE_TYPE_INTERNAL) }
    }

    @Test
    fun `given saveInternalAsset is called and contents are written, when storage type is updated, then propagates success`() {
        val file = mockk<File>()
        every { file.absolutePath } returns TEST_FILE_PATH

        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        every { fileSystem.writeToFile(file, any()) } returns Either.Right(file)
        coEvery { assetDao.updateStorageType(any(), any()) } returns Unit

        val result = runBlocking { assetLocalDataSource.saveInternalAsset(TEST_ID, mockk()) }

        result shouldSucceed { it shouldBeEqualTo TEST_FILE_PATH }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_ASSET_RELATIVE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, any()) }
        coVerify(exactly = 1) { assetDao.updateStorageType(TEST_ID, STORAGE_TYPE_INTERNAL) }
    }

    @Test
    fun `given assetPath is called & asset has storage type "internal", when fileSystem returns internal file, then propagates success`() {
        val assetEntity = mockk<AssetEntity>()
        every { assetEntity.id } returns TEST_ID
        every { assetEntity.storageType } returns STORAGE_TYPE_INTERNAL

        val file = mockk<File>()
        every { file.absolutePath } returns TEST_FILE_PATH
        every { fileSystem.internalFile(any()) } returns Either.Right(file)

        val result = assetLocalDataSource.assetPath(assetEntity)

        result shouldSucceed { it shouldBeEqualTo TEST_FILE_PATH }
        verify(exactly = 1) { fileSystem.internalFile(TEST_ASSET_RELATIVE_PATH) }
    }

    @Test
    fun `given assetPath is called & asset has storage type "internal", when fileSystem fails to return file, then propagates failure`() {
        val assetEntity = mockk<AssetEntity>()
        every { assetEntity.id } returns TEST_ID
        every { assetEntity.storageType } returns STORAGE_TYPE_INTERNAL

        val failure = mockk<Failure>()
        every { fileSystem.internalFile(any()) } returns Either.Left(failure)

        val result = assetLocalDataSource.assetPath(assetEntity)

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.internalFile(TEST_ASSET_RELATIVE_PATH) }
    }

    companion object {
        private const val TEST_ID = 12
        private const val TEST_ID_LONG = TEST_ID.toLong()
        private const val TEST_DOWNLOAD_KEY = "key-123"

        private const val TEST_ASSET_RELATIVE_PATH = "assets/$TEST_ID"
        private const val TEST_FILE_PATH = "storage/test files/$TEST_ASSET_RELATIVE_PATH"

        private const val STORAGE_TYPE_INTERNAL = "internal"
    }
}
