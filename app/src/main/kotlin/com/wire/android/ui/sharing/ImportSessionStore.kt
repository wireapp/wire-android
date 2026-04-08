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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.sharing

import android.content.Context
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.ForwardedAssetBundle
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.AttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun store(
        importedText: String?,
        importedAssets: List<ImportedMediaAsset>,
        forwardedAssets: List<ForwardedAssetBundle> = emptyList(),
    ): String = withContext(dispatchers.io()) {
        cleanupExpiredSessions()
        val sessionId = UUID.randomUUID().toString()
        val session = StoredImportSession(
            id = sessionId,
            createdAt = System.currentTimeMillis(),
            importedText = importedText,
            importedAssets = importedAssets.map { it.toStoredImportedAsset() },
            forwardedAssets = forwardedAssets.map { it.toStoredForwardedAsset() },
        )
        sessionFile(sessionId).apply {
            parentFile?.mkdirs()
            writeText(json.encodeToString(StoredImportSession.serializer(), session))
        }
        sessionId
    }

    suspend fun get(sessionId: String): ImportSession? = withContext(dispatchers.io()) {
        cleanupExpiredSessions()
        val file = sessionFile(sessionId)
        if (!file.exists()) {
            return@withContext null
        }

        runCatching {
            json.decodeFromString(StoredImportSession.serializer(), file.readText())
                .toImportSession()
        }.onFailure {
            appLogger.e("Failed to decode import session $sessionId", it)
        }.getOrNull()
    }

    suspend fun delete(sessionId: String) = withContext(dispatchers.io()) {
        sessionFile(sessionId).takeIf(File::exists)?.let { file ->
            runCatching {
                json.decodeFromString(StoredImportSession.serializer(), file.readText())
            }.onSuccess { session ->
                session.importedAssets.forEach { storedAsset ->
                    runCatching {
                        File(storedAsset.dataPath).delete()
                    }.onFailure { error ->
                        appLogger.w("Failed to delete imported asset ${storedAsset.dataPath}", error)
                    }
                }
            }
            file.delete()
        }
    }

    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        sessionsDirectory()
            .listFiles()
            ?.filter { now - it.lastModified() > SESSION_MAX_AGE_IN_MILLIS }
            ?.forEach { file ->
                runCatching {
                    val session = json.decodeFromString(StoredImportSession.serializer(), file.readText())
                    session.importedAssets.forEach { storedAsset -> File(storedAsset.dataPath).delete() }
                    file.delete()
                }.onFailure { error ->
                    appLogger.w("Failed to clean up stale import session ${file.name}", error)
                }
            }
    }

    private fun sessionsDirectory(): File = File(context.cacheDir, IMPORT_SESSIONS_DIRECTORY).apply { mkdirs() }

    private fun sessionFile(sessionId: String): File = File(sessionsDirectory(), "$sessionId.json")

    private fun StoredImportSession.toImportSession(): ImportSession =
        ImportSession(
            id = id,
            importedText = importedText,
            importedAssets = importedAssets.mapNotNull { storedAsset ->
                storedAsset.toImportedMediaAsset()
            },
            forwardedAssets = forwardedAssets.map { it.toForwardedAssetBundle() },
        )

    private fun StoredImportedAsset.toImportedMediaAsset(): ImportedMediaAsset? {
        val assetFile = File(dataPath)
        if (!assetFile.exists()) {
            appLogger.w("Imported asset no longer exists at $dataPath")
            return null
        }

        return ImportedMediaAsset(
            assetBundle = com.wire.android.ui.home.conversations.model.AssetBundle(
                key = key,
                mimeType = mimeType,
                dataPath = dataPath.toPath(),
                dataSize = dataSize,
                fileName = fileName,
                assetType = AttachmentType.valueOf(assetType),
                audioWavesMask = audioWavesMask
            ),
            assetSizeExceeded = assetSizeExceeded
        )
    }

    private fun ImportedMediaAsset.toStoredImportedAsset(): StoredImportedAsset =
        StoredImportedAsset(
            key = assetBundle.key,
            mimeType = assetBundle.mimeType,
            dataPath = assetBundle.dataPath.toString(),
            dataSize = assetBundle.dataSize,
            fileName = assetBundle.fileName,
            assetType = assetBundle.assetType.name,
            audioWavesMask = assetBundle.audioWavesMask,
            assetSizeExceeded = assetSizeExceeded
        )

    private fun ForwardedAssetBundle.toStoredForwardedAsset() =
        StoredForwardedAsset(
            assetId = assetId,
            assetToken = assetToken,
            assetDomain = assetDomain,
            otrKey = otrKey,
            sha256 = sha256,
            encryptionAlgorithm = encryptionAlgorithm,
            fileName = fileName,
            mimeType = mimeType,
            dataSize = dataSize,
            assetType = assetType.name,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            durationMs = durationMs,
            audioNormalizedLoudness = audioNormalizedLoudness,
            localDataPath = localDataPath,
        )

    private fun StoredForwardedAsset.toForwardedAssetBundle() =
        ForwardedAssetBundle(
            assetId = assetId,
            assetToken = assetToken,
            assetDomain = assetDomain,
            otrKey = otrKey,
            sha256 = sha256,
            encryptionAlgorithm = encryptionAlgorithm,
            fileName = fileName,
            mimeType = mimeType,
            dataSize = dataSize,
            assetType = AttachmentType.valueOf(assetType),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            durationMs = durationMs,
            audioNormalizedLoudness = audioNormalizedLoudness,
            localDataPath = localDataPath,
        )

    companion object {
        private const val IMPORT_SESSIONS_DIRECTORY = "incoming-share/sessions"
        private const val SESSION_MAX_AGE_IN_MILLIS = 60 * 60 * 1000L
        private val json = Json { ignoreUnknownKeys = true }
    }
}

data class ImportSession(
    val id: String,
    val importedText: String?,
    val importedAssets: List<ImportedMediaAsset>,
    val forwardedAssets: List<ForwardedAssetBundle> = emptyList(),
)

@Serializable
private data class StoredImportSession(
    val id: String,
    val createdAt: Long,
    val importedText: String?,
    val importedAssets: List<StoredImportedAsset>,
    val forwardedAssets: List<StoredForwardedAsset> = emptyList(),
)

@Serializable
private data class StoredImportedAsset(
    val key: String,
    val mimeType: String,
    val dataPath: String,
    val dataSize: Long,
    val fileName: String,
    val assetType: String,
    val audioWavesMask: List<Int>? = null,
    val assetSizeExceeded: Int? = null,
)

@Serializable
private data class StoredForwardedAsset(
    val assetId: String,
    val assetToken: String? = null,
    val assetDomain: String? = null,
    val otrKey: ByteArray,
    val sha256: ByteArray,
    val encryptionAlgorithm: String? = null,
    val fileName: String,
    val mimeType: String,
    val dataSize: Long,
    val assetType: String,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val durationMs: Long? = null,
    val audioNormalizedLoudness: ByteArray? = null,
    val localDataPath: String? = null,
)
