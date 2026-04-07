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
package com.wire.android.ui.home.conversations.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.util.accountsRootDir
import com.wire.android.util.getProviderAuthority
import com.wire.android.util.parcelable
import com.wire.android.util.parcelableArrayList
import com.wire.android.util.shareableCacheFile
import com.wire.android.util.shareableFilesFile
import com.wire.android.util.pathToUri
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HandleUriAssetUseCaseInstrumentedTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var handleUriAssetUseCase: HandleUriAssetUseCase

    private lateinit var context: Context
    private lateinit var internalDatabaseFile: File
    private lateinit var nonShareableInternalFile: File
    private lateinit var shareableInternalFile: File
    private lateinit var nonShareableCacheFile: File
    private lateinit var shareableCacheBackedFile: File
    private lateinit var nonShareableAccountsFile: File
    private lateinit var proteusKeystoreFile: File
    private lateinit var mlsKeystoreFile: File

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        internalDatabaseFile = context.getDatabasePath(TEST_DATABASE_NAME).apply {
            parentFile?.mkdirs()
            writeText("top secret database contents")
        }
        nonShareableInternalFile = File(context.filesDir, TEST_NON_SHAREABLE_FILES_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("persistent internal data that should not be directly exposed")
        }
        shareableInternalFile = context.shareableFilesFile(TEST_SHARED_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("safe shared contents")
        }
        nonShareableCacheFile = File(context.cacheDir, TEST_NON_SHAREABLE_CACHE_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("cache data that should stay private")
        }
        shareableCacheBackedFile = context.shareableCacheFile(TEST_SHAREABLE_CACHE_FILE_NAME).apply {
            writeText("cache data explicitly prepared for sharing")
        }
        nonShareableAccountsFile = File(context.accountsRootDir(), TEST_NON_SHAREABLE_ACCOUNTS_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("private accounts data")
        }
        proteusKeystoreFile = File(context.accountsRootDir(), "wire.test/user/proteus/keystore/keystore").apply {
            parentFile?.mkdirs()
            writeText("proteus keystore")
        }
        mlsKeystoreFile = File(context.accountsRootDir(), "wire.test/user/mls/client-1/keystore/keystore").apply {
            parentFile?.mkdirs()
            writeText("mls keystore")
        }
    }

    @After
    fun tearDown() {
        if (::internalDatabaseFile.isInitialized) {
            internalDatabaseFile.delete()
        }
        if (::nonShareableInternalFile.isInitialized) {
            nonShareableInternalFile.delete()
        }
        if (::shareableInternalFile.isInitialized) {
            shareableInternalFile.delete()
        }
        if (::nonShareableCacheFile.isInitialized) {
            nonShareableCacheFile.delete()
        }
        if (::shareableCacheBackedFile.isInitialized) {
            shareableCacheBackedFile.delete()
        }
        if (::nonShareableAccountsFile.isInitialized) {
            nonShareableAccountsFile.delete()
        }
        if (::proteusKeystoreFile.isInitialized) {
            proteusKeystoreFile.delete()
        }
        if (::mlsKeystoreFile.isInitialized) {
            mlsKeystoreFile.delete()
        }
    }

    @Test
    fun givenShareIntentWithInternalDatabaseFileUri_whenHandlingSharedAsset_thenItIsRejected() = runTest {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(internalDatabaseFile))
        }

        val sharedUri = shareIntent.parcelable<Uri>(Intent.EXTRA_STREAM)

        assertNotNull(sharedUri)

        val result = handleUriAssetUseCase.invoke(sharedUri!!, saveToDeviceIfInvalid = false)

        assertTrue(result is HandleUriAssetUseCase.Result.Failure.Unknown)
    }

    @Test
    fun givenShareIntentWithFileProviderUri_whenHandlingSharedAsset_thenItIsImported() = runTest {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, shareableContentUri())
        }

        val sharedUri = shareIntent.parcelable<Uri>(Intent.EXTRA_STREAM)

        assertNotNull(sharedUri)

        val result = handleUriAssetUseCase.invoke(sharedUri!!, saveToDeviceIfInvalid = false)

        assertTrue(result is HandleUriAssetUseCase.Result.Success)
        assertEquals(TEST_SHARED_FILE_NAME, (result as HandleUriAssetUseCase.Result.Success).assetBundle.fileName)
    }

    @Test
    fun givenMultipleShareIntentWithMixedUris_whenHandlingSharedAssets_thenOnlyContentUriIsImported() = runTest {
        val validContentUri = shareableContentUri()
        val invalidInternalFileUri = Uri.fromFile(internalDatabaseFile)
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(validContentUri, invalidInternalFileUri))
        }

        val sharedUris = shareIntent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM).orEmpty()

        assertEquals(2, sharedUris.size)

        val results = sharedUris.map { uri ->
            handleUriAssetUseCase.invoke(uri, saveToDeviceIfInvalid = false)
        }

        assertTrue(results[0] is HandleUriAssetUseCase.Result.Success)
        assertTrue(results[1] is HandleUriAssetUseCase.Result.Failure.Unknown)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenPlainCacheFile_whenCreatingProviderUri_thenItIsNotExposed() {
        FileProvider.getUriForFile(context, context.getProviderAuthority(), nonShareableCacheFile)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenPlainInternalFile_whenCreatingProviderUri_thenItIsNotExposed() {
        FileProvider.getUriForFile(context, context.getProviderAuthority(), nonShareableInternalFile)
    }

    @Test
    fun givenPlainAccountsFile_whenCreatingProviderUri_thenItIsNotExposed() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.getProviderAuthority(), nonShareableAccountsFile)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenPlainAccountsFile_whenConvertingPathToUri_thenItIsRejected() {
        context.pathToUri(nonShareableAccountsFile.toPath().toOkioPath(), nonShareableAccountsFile.name)
    }

    @Test
    fun givenShareableCacheFile_whenCreatingProviderUri_thenItRemainsShareable() {
        val uri = FileProvider.getUriForFile(context, context.getProviderAuthority(), shareableCacheBackedFile)

        assertEquals("content", uri.scheme)
        assertNotEquals(nonShareableCacheFile.absolutePath, shareableCacheBackedFile.absolutePath)
    }

    @Test
    fun givenPlainInternalFile_whenConvertingPathToUri_thenItIsCopiedToShareableFilesDirectory() {
        val uri = context.pathToUri(nonShareableInternalFile.toPath().toOkioPath(), nonShareableInternalFile.name)

        assertEquals("content", uri.scheme)
        val copiedContents = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        assertEquals("persistent internal data that should not be directly exposed", copiedContents)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenDatabaseFile_whenConvertingPathToUri_thenItIsRejected() {
        context.pathToUri(internalDatabaseFile.toPath().toOkioPath(), internalDatabaseFile.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenProteusKeystoreFileUnderAccountsRoot_whenConvertingPathToUri_thenItIsRejected() {
        context.pathToUri(proteusKeystoreFile.toPath().toOkioPath(), proteusKeystoreFile.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenMlsKeystoreFileUnderAccountsRoot_whenConvertingPathToUri_thenItIsRejected() {
        context.pathToUri(mlsKeystoreFile.toPath().toOkioPath(), mlsKeystoreFile.name)
    }

    private fun shareableContentUri(): Uri =
        FileProvider.getUriForFile(context, context.getProviderAuthority(), shareableInternalFile)

    companion object {
        private const val TEST_DATABASE_NAME = "share-intent-sqlcipher-test.db"
        private const val TEST_SHARED_FILE_NAME = "share-intent-safe.txt"
        private const val TEST_NON_SHAREABLE_FILES_FILE_NAME = "private-files.txt"
        private const val TEST_NON_SHAREABLE_CACHE_FILE_NAME = "private-cache.txt"
        private const val TEST_NON_SHAREABLE_ACCOUNTS_FILE_NAME = "private-accounts.txt"
        private const val TEST_SHAREABLE_CACHE_FILE_NAME = "shareable-cache.txt"
    }
}
