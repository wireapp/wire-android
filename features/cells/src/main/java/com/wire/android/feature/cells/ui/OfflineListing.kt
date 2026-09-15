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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.feature.cells.ui

import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo

/**
 * A single row of the listing shown while browsing files saved for offline use.
 */
internal sealed interface OfflineListingEntry {

    data class FileEntry(val info: OfflineFileInfo) : OfflineListingEntry

    /**
     * Folder rebuilt from the remote paths of the offline files stored inside it.
     * [modifiedTime] is the most recent modification time found in it.
     */
    data class FolderEntry(
        val name: String,
        val remotePath: String,
        val modifiedTime: Long?,
    ) : OfflineListingEntry
}

/**
 * Rebuilds the content of the folder being browsed from the flat list of files saved for offline use.
 *
 * Offline files keep the path they have on the server, so the folder structure of a conversation can
 * be reproduced without network access: files stored directly in [currentPath] are listed as files,
 * and files stored deeper recreate the folder they are nested in.
 *
 * @param currentPath remote path of the folder being browsed: the conversation id at the root of a
 * conversation, `"conversationId/folder/..."` inside a folder, and `null` for All files, which lists
 * every offline file as a flat, newest-first list because it has no folder navigation.
 */
internal fun buildOfflineListing(
    currentPath: String?,
    offlineFiles: List<OfflineFileInfo>,
): List<OfflineListingEntry> {
    if (currentPath == null) {
        return offlineFiles
            .sortedByDescending { it.modifiedAt ?: it.downloadedAt }
            .map { OfflineListingEntry.FileEntry(it) }
    }

    val files = mutableListOf<OfflineFileInfo>()
    val folders = linkedMapOf<String, Long?>()

    offlineFiles.forEach { info ->
        val relativePath = info.relativePathIn(currentPath) ?: return@forEach
        val folderName = relativePath.substringBefore('/')

        if (folderName == relativePath) {
            files += info
        } else {
            folders[folderName] = maxOfNullable(folders[folderName], info.modifiedAt)
        }
    }

    val folderEntries = folders.entries
        .sortedBy { it.key.lowercase() }
        .map { (name, modifiedTime) ->
            OfflineListingEntry.FolderEntry(
                name = name,
                remotePath = "$currentPath/$name",
                modifiedTime = modifiedTime,
            )
        }

    return folderEntries + files.sortedBy { it.name.lowercase() }.map { OfflineListingEntry.FileEntry(it) }
}

/**
 * Path of this file relative to the folder at [currentPath], or null when it is not stored in it.
 */
private fun OfflineFileInfo.relativePathIn(currentPath: String): String? {
    val remotePath = remotePath
        ?: return name.takeIf { currentPath == conversationId }

    return remotePath
        .takeIf { it.startsWith("$currentPath/") }
        ?.removePrefix("$currentPath/")
        ?.takeIf { it.isNotBlank() }
}

private fun maxOfNullable(first: Long?, second: Long?): Long? =
    when {
        first == null -> second
        second == null -> first
        else -> maxOf(first, second)
    }
