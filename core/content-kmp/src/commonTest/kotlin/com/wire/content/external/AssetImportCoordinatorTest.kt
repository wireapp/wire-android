/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.content.external

import com.wire.content.asset.PreparedAsset
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AssetImportCoordinatorTest {
    @Test
    fun `accepts an asset exactly at the limit and preserves its display name`() = runTest {
        val fileSystem = TestFileSystem()
        val coordinator = coordinator(fileSystem, size = 100, limit = 100, fileName = "../report.pdf")

        val result = coordinator(request())

        val success = assertIs<ExternalContentImportResult.Success>(result)
        assertEquals("../report.pdf", success.asset.fileName)
        assertTrue(fileSystem.exists(success.asset.dataPath))
    }

    @Test
    fun `exports requested oversized media and returns the byte limit`() = runTest {
        val fileSystem = TestFileSystem()
        val exporter = TestExporter()
        val coordinator = coordinator(fileSystem, size = 101, limit = 100, fileName = "../asset.bin", exporter = exporter)

        val result = coordinator(request(saveToDevice = true))

        val tooLarge = assertIs<ExternalContentImportResult.TooLarge>(result)
        assertEquals(100, tooLarge.maximumSizeBytes)
        assertEquals(tooLarge.asset.dataPath, exporter.mediaExport?.path)
        assertEquals("_asset.bin", exporter.mediaExport?.displayName)
    }

    @Test
    fun `removes a temporary file when reading fails`() = runTest {
        val fileSystem = TestFileSystem()
        val destination = fileSystem.tempFilePath(KEY)
        val reader = object : TestReader(fileSystem, 1) {
            override suspend fun prepare(
                request: ExternalContentImportRequest,
                destination: Path,
                assetKey: String,
            ): PlatformResult<PreparedAsset> {
                fileSystem.sink(destination).buffer().use { it.writeUtf8("partial") }
                return PlatformResult.Failure("broken")
            }
        }
        val coordinator = coordinator(fileSystem, reader = reader)

        assertIs<ExternalContentImportResult.Failure>(coordinator(request()))
        assertFalse(fileSystem.exists(destination))
    }

    @Test
    fun `removes a temporary file when selection is cancelled`() = runTest {
        val fileSystem = TestFileSystem()
        val destination = fileSystem.tempFilePath(KEY)
        val reader = object : TestReader(fileSystem, 1) {
            override suspend fun prepare(
                request: ExternalContentImportRequest,
                destination: Path,
                assetKey: String,
            ): PlatformResult<PreparedAsset> {
                fileSystem.sink(destination).buffer().use { it.writeUtf8("partial") }
                return PlatformResult.Cancelled
            }
        }

        assertIs<ExternalContentImportResult.Cancelled>(coordinator(fileSystem, reader = reader)(request()))
        assertFalse(fileSystem.exists(destination))
    }

    @Test
    fun `removes a partial temporary file and propagates coroutine cancellation`() = runTest {
        val fileSystem = TestFileSystem()
        val destination = fileSystem.tempFilePath(KEY)
        val reader = object : TestReader(fileSystem, 1) {
            override suspend fun prepare(
                request: ExternalContentImportRequest,
                destination: Path,
                assetKey: String,
            ): PlatformResult<PreparedAsset> {
                fileSystem.sink(destination).buffer().use { it.writeUtf8("partial") }
                throw CancellationException("cancelled")
            }
        }

        assertFailsWith<CancellationException> { coordinator(fileSystem, reader = reader)(request()) }
        assertFalse(fileSystem.exists(destination))
    }

    @Suppress("LongParameterList")
    private fun coordinator(
        fileSystem: TestFileSystem,
        size: Long = 1,
        limit: Long = 100,
        fileName: String = "asset.bin",
        exporter: TestExporter = TestExporter(),
        reader: ExternalContentReader = TestReader(fileSystem, size, fileName),
    ) = AssetImportCoordinator(
        reader = reader,
        exporter = exporter,
        fileSystem = fileSystem,
        sizeLimitProvider = AssetSizeLimitProvider { limit },
        keyGenerator = ContentKeyGenerator { KEY },
    )

    private fun request(saveToDevice: Boolean = false) = ExternalContentImportRequest(
        reference = ExternalContentReference("opaque-reference"),
        saveToDeviceIfInvalid = saveToDevice,
    )

    private open class TestReader(
        private val fileSystem: TestFileSystem,
        private val size: Long,
        private val fileName: String = "asset.bin",
    ) : ExternalContentReader {
        override suspend fun prepare(
            request: ExternalContentImportRequest,
            destination: Path,
            assetKey: String,
        ): PlatformResult<PreparedAsset> {
            fileSystem.sink(destination).buffer().use { it.writeUtf8("asset") }
            return PlatformResult.Success(
                PreparedAsset(assetKey, "application/octet-stream", destination, size, fileName, AttachmentType.GENERIC_FILE)
            )
        }

        override suspend fun copyToPrivateStorage(
            source: ExternalContentReference,
            destination: Path,
        ): PlatformResult<Long> = PlatformResult.Unsupported
    }

    private class TestExporter : FileExporter {
        var mediaExport: LocalContent? = null

        override suspend fun exportToDownloads(content: LocalContent): PlatformResult<String?> = PlatformResult.Unsupported
        override suspend fun exportToMedia(content: LocalContent): PlatformResult<String?> {
            mediaExport = content
            return PlatformResult.Success(content.displayName)
        }
        override suspend fun write(
            content: LocalContent,
            destination: ExternalContentReference,
        ): PlatformResult<Unit> = PlatformResult.Unsupported
        override suspend fun openDownloadSink(displayName: String): PlatformResult<Sink> = PlatformResult.Unsupported
    }

    private class TestFileSystem : KaliumFileSystem {
        private val delegate = FakeFileSystem().apply {
            allowDeletingOpenFiles = true
            createDirectories("/cache".toPath())
            createDirectories("/data".toPath())
        }
        override val rootCachePath = "/cache".toPath()
        override val rootDBPath = "/data".toPath()
        override fun sink(outputPath: Path, mustCreate: Boolean): Sink = delegate.sink(outputPath, mustCreate)
        override fun source(inputPath: Path): Source = delegate.source(inputPath)
        override fun createDirectories(dir: Path) = delegate.createDirectories(dir)
        override fun createDirectory(dir: Path, mustCreate: Boolean) = delegate.createDirectory(dir, mustCreate)
        override fun delete(path: Path, mustExist: Boolean) = delegate.delete(path, mustExist)
        override fun deleteContents(dir: Path, mustExist: Boolean) = delegate.deleteRecursively(dir, mustExist)
        override fun exists(path: Path): Boolean = delegate.exists(path)
        override fun copy(sourcePath: Path, targetPath: Path) = delegate.copy(sourcePath, targetPath)
        override fun tempFilePath(pathString: String?): Path = "$rootCachePath/${pathString ?: "temp"}".toPath()
        override fun providePersistentAssetPath(assetName: String): Path = "/data/$assetName".toPath()
        override fun selfUserAvatarPath(): Path = providePersistentAssetPath("avatar.jpg")
        override suspend fun readByteArray(inputPath: Path): ByteArray = source(inputPath).buffer().use { it.readByteArray() }
        override suspend fun writeData(outputSink: Sink, dataSource: Source): Long =
            outputSink.buffer().use { it.writeAll(dataSource) }
        override suspend fun listDirectories(dir: Path): List<Path> = delegate.list(dir)
        override fun size(path: Path): Long? = delegate.metadata(path).size
    }

    private companion object {
        const val KEY = "generated-key"
    }
}
