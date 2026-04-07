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
    suspend fun store(importedText: String?, importedAssets: List<ImportedMediaAsset>): String = withContext(dispatchers.io()) {
        cleanupExpiredSessions()
        val sessionId = UUID.randomUUID().toString()
        val session = StoredImportSession(
            id = sessionId,
            createdAt = System.currentTimeMillis(),
            importedText = importedText,
            importedAssets = importedAssets.map { it.toStoredImportedAsset() }
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
            }
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
)

@Serializable
private data class StoredImportSession(
    val id: String,
    val createdAt: Long,
    val importedText: String?,
    val importedAssets: List<StoredImportedAsset>,
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
