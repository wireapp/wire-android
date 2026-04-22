/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.aiassistant.download

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BuildConfigHuggingFaceTokenProviderTest {

    @Test
    fun givenTokenIsNull_whenGettingDownloadAuthorization_thenNullIsReturned() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = null,
            baseUrl = DEFAULT_BASE_URL
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertNull(authorization)
    }

    @Test
    fun givenTokenIsBlank_whenGettingDownloadAuthorization_thenNullIsReturned() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = " ",
            baseUrl = DEFAULT_BASE_URL
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertNull(authorization)
    }

    @Test
    fun givenTokenExists_whenGettingDownloadAuthorization_thenTokenIsReturned() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = "hf_token",
            baseUrl = DEFAULT_BASE_URL
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertEquals("hf_token", authorization?.token)
    }

    @Test
    fun givenDefaultBaseUrl_whenGettingDownloadAuthorization_thenDefaultDownloadUrlIsReturned() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = "hf_token",
            baseUrl = DEFAULT_BASE_URL
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertEquals(DEFAULT_DOWNLOAD_URL, authorization?.downloadUrl)
    }

    @Test
    fun givenCustomBaseUrl_whenGettingDownloadAuthorization_thenCustomDownloadUrlIsReturned() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = "hf_token",
            baseUrl = "https://models.example.com"
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertEquals(
            "https://models.example.com/google/test-model/resolve/test-revision/model/test-model.litertlm",
            authorization?.downloadUrl
        )
    }

    @Test
    fun givenBaseUrlHasTrailingSlash_whenGettingDownloadAuthorization_thenDownloadUrlDoesNotContainDoubleSlash() = runTest {
        val provider = BuildConfigHuggingFaceTokenProvider(
            token = "hf_token",
            baseUrl = "$DEFAULT_BASE_URL/"
        )

        val authorization = provider.getDownloadAuthorization(DESCRIPTOR)

        assertEquals(DEFAULT_DOWNLOAD_URL, authorization?.downloadUrl)
    }

    private companion object {
        const val DEFAULT_BASE_URL = "https://huggingface.co"
        const val DEFAULT_DOWNLOAD_URL =
            "https://huggingface.co/google/test-model/resolve/test-revision/model/test-model.litertlm"

        val DESCRIPTOR = AiModelDescriptor(
            displayName = "Test model",
            repositoryId = "google/test-model",
            artifactPath = "model/test-model.litertlm",
            localDirectoryName = "test-model",
            localFileName = "model.litertlm",
            revision = "test-revision"
        )
    }
}
