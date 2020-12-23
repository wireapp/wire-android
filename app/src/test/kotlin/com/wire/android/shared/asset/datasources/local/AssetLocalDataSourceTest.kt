package com.wire.android.shared.asset.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FileDoesNotExist
import com.wire.android.core.functional.Either
import com.wire.android.core.io.FileSystem
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
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
    fun `given assetById is called, when dao call fails to return an entity, then propagates failure`() {
        coEvery { assetDao.assetById(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail {}
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given assetById is called and dao call returned an asset, when asset has no storage path, then fails with FileDoesNotExist`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity
        every { assetEntity.storagePath } returns null

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail { it shouldBeEqualTo FileDoesNotExist }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
        verify { fileSystem wasNot Called }
    }

    @Test
    fun `given assetById is called and entity has a storage path, when fileSystem fails to check path, then propagates failure`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity
        every { assetEntity.storagePath } returns TEST_FILE_PATH
        val failure = mockk<Failure>()
        every { fileSystem.internalFile(any()) } returns Either.Left(failure)

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
        verify(exactly = 1) { fileSystem.internalFile(TEST_FILE_PATH) }
    }

    @Test
    fun `given assetById is called and entity has a storage path, when fileSystem finds the file in path, then propagates success`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity
        every { assetEntity.storagePath } returns TEST_FILE_PATH
        val file = mockk<File>()
        every { fileSystem.internalFile(any()) } returns Either.Right(file)

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldSucceed { it shouldBeEqualTo file }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
        verify(exactly = 1) { fileSystem.internalFile(TEST_FILE_PATH) }
    }

    @Test
    fun `given asset is called, when entity has no storage path, then fails with FileDoesNotExist`() {
        val assetEntity = mockk<AssetEntity>()
        every { assetEntity.storagePath } returns null

        val result = runBlocking { assetLocalDataSource.asset(assetEntity) }

        result shouldFail { it shouldBeEqualTo FileDoesNotExist }
        verify { fileSystem wasNot Called }
    }

    @Test
    fun `given asset is called and assetEntity has a storage path, when fileSystem fails to check path, then propagates failure`() {
        val assetEntity = mockk<AssetEntity>()
        every { assetEntity.storagePath } returns TEST_FILE_PATH
        val failure = mockk<Failure>()
        every { fileSystem.internalFile(any()) } returns Either.Left(failure)

        val result = runBlocking { assetLocalDataSource.asset(assetEntity) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.internalFile(TEST_FILE_PATH) }
    }

    @Test
    fun `given asset is called and assetEntity has a storage path, when fileSystem finds the file in path, then propagates success`() {
        val assetEntity = mockk<AssetEntity>()
        every { assetEntity.storagePath } returns TEST_FILE_PATH
        val file = mockk<File>()
        every { fileSystem.internalFile(any()) } returns Either.Right(file)

        val result = runBlocking { assetLocalDataSource.asset(assetEntity) }

        result shouldSucceed { it shouldBeEqualTo file }
        verify(exactly = 1) { fileSystem.internalFile(TEST_FILE_PATH) }
    }

    @Test
    fun `given createAssets is called with download keys, then calls assetDao to insert entities with proper keys`() {
        coEvery { assetDao.insertAll(any()) } returns TEST_ROW_IDS

        val result = runBlocking { assetLocalDataSource.createAssets(TEST_DOWNLOAD_KEYS) }

        val insertedAssetsSlot = slot<List<AssetEntity>>()
        coVerify(exactly = 1) { assetDao.insertAll(capture(insertedAssetsSlot)) }
        insertedAssetsSlot.captured.let {
            it.mapIndexed { index, assetEntity -> assetEntity.downloadKey shouldBeEqualTo TEST_DOWNLOAD_KEYS[index] }
        }
    }

    @Test
    fun `given createAssets is called, when dao inserts new assets with keys, then propagates their ids as success`() {
        coEvery { assetDao.insertAll(any()) } returns TEST_ROW_IDS

        val result = runBlocking { assetLocalDataSource.createAssets(TEST_DOWNLOAD_KEYS) }

        result shouldSucceed { it shouldBeEqualTo TEST_ROW_IDS.map { it.toInt() } }
    }

    @Test
    fun `given createAssets is called, when dao call fails, then propagates failure`() {
        coEvery { assetDao.insertAll(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.createAssets(TEST_DOWNLOAD_KEYS) }

        result shouldFail {}
    }

    @Test
    fun `given saveAsset is called, when internal file cannot be created, then propagates failure`() {
        val failure = mockk<Failure>()
        every { fileSystem.createInternalFile(any()) } returns Either.Left(failure)

        val result = runBlocking { assetLocalDataSource.saveAsset(TEST_ID, mockk(), TEST_FILE_PATH) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_FILE_PATH) }
    }

    @Test
    fun `given saveAsset is called and internal file is created, when file cannot be written, then propagates failure`() {
        val file = mockk<File>()
        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        val failure = mockk<Failure>()
        every { fileSystem.writeToFile(file, any()) } returns Either.Left(failure)

        val inputStream = mockk<InputStream>()
        val result = runBlocking { assetLocalDataSource.saveAsset(TEST_ID, inputStream, TEST_FILE_PATH) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_FILE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, inputStream) }
    }

    @Test
    fun `given saveAsset is called and contents are written, when asset path cannot be updated, then propagates failure`() {
        val file = mockk<File>()
        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        every { fileSystem.writeToFile(file, any()) } returns Either.Right(file)
        coEvery { assetDao.updatePath(any(), any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.saveAsset(TEST_ID, mockk(), TEST_FILE_PATH) }

        result shouldFail { }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_FILE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, any()) }
        coVerify(exactly = 1) { assetDao.updatePath(TEST_ID, TEST_FILE_PATH) }
    }

    @Test
    fun `given saveAsset is called and contents are written, when asset path is updated, then propagates success`() {
        val file = mockk<File>()
        every { file.absolutePath } returns TEST_FILE_PATH

        every { fileSystem.createInternalFile(any()) } returns Either.Right(file)
        every { fileSystem.writeToFile(file, any()) } returns Either.Right(file)
        coEvery { assetDao.updatePath(any(), any()) } returns Unit

        val result = runBlocking { assetLocalDataSource.saveAsset(TEST_ID, mockk(), TEST_FILE_PATH) }

        result shouldSucceed { it shouldBeEqualTo file }
        verify(exactly = 1) { fileSystem.createInternalFile(TEST_FILE_PATH) }
        verify(exactly = 1) { fileSystem.writeToFile(file, any()) }
        coVerify(exactly = 1) { assetDao.updatePath(TEST_ID, TEST_FILE_PATH) }
    }

    @Test
    fun `given downloadKey is called, when dao returns a key, then propagates success`() {
        coEvery { assetDao.downloadKey(any()) } returns "download_key"

        val result = runBlocking { assetLocalDataSource.downloadKey(TEST_ID) }

        result shouldSucceed { it shouldBeEqualTo "download_key" }
        coVerify(exactly = 1) { assetDao.downloadKey(TEST_ID) }
    }

    @Test
    fun `given downloadKey is called, when dao operation fails, then propagates failure`() {
        coEvery { assetDao.downloadKey(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.downloadKey(TEST_ID) }

        result shouldFail { }
        coVerify(exactly = 1) { assetDao.downloadKey(TEST_ID) }

    }

    companion object {
        private const val TEST_ID = 12
        private val TEST_ROW_IDS = listOf(12L, 13L, 14L, 15L)
        private val TEST_DOWNLOAD_KEYS = listOf("key-1", null, "key-2", "key-3")

        private const val TEST_FILE_PATH = "storage/test files/asset.png"
    }
}
