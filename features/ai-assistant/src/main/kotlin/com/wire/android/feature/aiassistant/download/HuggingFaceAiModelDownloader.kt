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
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.FailureReason
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import com.wire.android.util.dispatchers.DispatcherProvider
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class HuggingFaceAiModelDownloader @Inject constructor(
    private val tokenProvider: HuggingFaceTokenProvider,
    private val httpClient: AiModelHttpClient,
    private val storage: AiModelStorage,
    private val dispatchers: DispatcherProvider
) : AiModelDownloader {

    override fun download(descriptor: AiModelDescriptor): Flow<AiModelDownloadState> = flow {
        emit(AiModelDownloadState.Starting)

        val authorization = tokenProvider.getDownloadAuthorization(descriptor)
        if (authorization == null) {
            emit(AiModelDownloadState.AuthRequired())
            return@flow
        }

        val finalFile = storage.getModelFile(descriptor)
        val tempFile = storage.getTempModelFile(descriptor)

        try {
            storage.ensureModelDirectoryExists(descriptor)
            tempFile.deleteIfExists()

            httpClient.open(
                url = authorization.downloadUrl,
                headers = mapOf(AUTHORIZATION_HEADER to "$BEARER_PREFIX ${authorization.token}")
            ).use { response ->
                when (response.code) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> {
                        tempFile.deleteIfExists()
                        emit(AiModelDownloadState.AuthRequired(response.errorBody))
                    }
                    in HTTP_SUCCESS_RANGE -> {
                        val authorizationMessage = response.readAuthorizationMessageIfPresent()
                        if (authorizationMessage != null) {
                            tempFile.deleteIfExists()
                            emit(AiModelDownloadState.AuthRequired(authorizationMessage))
                            return@use
                        }
                        emit(AiModelDownloadState.Downloading(0F))
                        response.copyToTempFile(tempFile) { progress ->
                            emit(AiModelDownloadState.Downloading(progress))
                        }
                        val validationResult = validateDownloadedFile(tempFile, descriptor)
                        if (validationResult != null) {
                            tempFile.deleteIfExists()
                            emit(AiModelDownloadState.Failed(validationResult))
                            return@use
                        }
                        storage.promoteTempFile(descriptor)
                        emit(AiModelDownloadState.Ready(finalFile.absolutePath))
                    }
                    else -> {
                        tempFile.deleteIfExists()
                        val authorizationMessage = response.errorBody?.extractAuthorizationMessage()
                        if (authorizationMessage != null) {
                            emit(AiModelDownloadState.AuthRequired(authorizationMessage))
                        } else {
                            emit(AiModelDownloadState.Failed(FailureReason.InvalidResponse))
                        }
                    }
                }
            }
        } catch (exception: CancellationException) {
            tempFile.deleteIfExists()
            throw exception
        } catch (exception: IOException) {
            tempFile.deleteIfExists()
            emit(AiModelDownloadState.Failed(FailureReason.Network))
        } catch (exception: SecurityException) {
            tempFile.deleteIfExists()
            emit(AiModelDownloadState.Failed(FailureReason.Storage))
        }
    }.flowOn(dispatchers.io())

    private suspend fun AiModelHttpResponse.copyToTempFile(
        tempFile: File,
        onProgress: suspend (Float?) -> Unit
    ) {
        val totalBytes = contentLength?.takeIf { it > 0L }
        var copiedBytes = 0L
        inputStream().use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    copiedBytes += bytesRead
                    onProgress(totalBytes?.let { copiedBytes.toFloat() / it.toFloat() })
                    bytesRead = input.read(buffer)
                }
            }
        }
    }

    private fun AiModelHttpResponse.readAuthorizationMessageIfPresent(): String? {
        if (!isTextResponse()) return null
        return inputStream()
            .bufferedReader()
            .use { reader -> reader.readText() }
            .extractAuthorizationMessage()
    }

    private fun AiModelHttpResponse.isTextResponse(): Boolean {
        val normalizedContentType = contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
        return normalizedContentType in TEXT_RESPONSE_CONTENT_TYPES
    }

    private fun String.extractAuthorizationMessage(): String? {
        val normalized = replace(HTML_TAG_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: return null

        return normalized.takeIf { message ->
            HUGGING_FACE_HOST in message && (
                "ask for access" in message.lowercase(Locale.ROOT) ||
                    "authorized list" in message.lowercase(Locale.ROOT) ||
                    "access to model" in message.lowercase(Locale.ROOT)
                )
        }
    }

    private fun validateDownloadedFile(tempFile: File, descriptor: AiModelDescriptor): FailureReason? {
        val hasUnexpectedSize = descriptor.expectedByteSize?.let { expectedByteSize ->
            tempFile.length() != expectedByteSize
        } ?: false
        val hasUnexpectedChecksum = descriptor.sha256?.let { expectedChecksum ->
            !expectedChecksum.equals(tempFile.sha256(), ignoreCase = true)
        } ?: false

        return when {
            hasUnexpectedSize -> FailureReason.InvalidResponse
            hasUnexpectedChecksum -> FailureReason.InvalidChecksum
            else -> null
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance(SHA_256)
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead = input.read(buffer)
            while (bytesRead >= 0) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun File.deleteIfExists() {
        if (exists()) {
            delete()
        }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer"
        const val HUGGING_FACE_HOST = "https://huggingface.co/"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val SHA_256 = "SHA-256"

        val HTML_TAG_REGEX = Regex("<[^>]+>")
        val HTTP_SUCCESS_RANGE = 200..299
        val TEXT_RESPONSE_CONTENT_TYPES = setOf("text/html", "text/plain", "application/json")
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
