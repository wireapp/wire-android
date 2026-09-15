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
package com.wire.android.feature.cells.ui

import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import org.junit.Test
import kotlin.test.assertEquals

class OfflineListingTest {

    @Test
    fun `given file saved in a subfolder when listing the conversation root then the folder is listed`() {
        val listing = buildOfflineListing(
            currentPath = CONVERSATION_ID,
            offlineFiles = listOf(offlineFile(id = "1", name = "report.pdf", path = "$CONVERSATION_ID/reports/report.pdf")),
        )

        assertEquals(
            listOf(OfflineListingEntry.FolderEntry(name = "reports", remotePath = "$CONVERSATION_ID/reports", modifiedTime = 100L)),
            listing,
        )
    }

    @Test
    fun `given file saved in a subfolder when listing that folder then the file is listed`() {
        val file = offlineFile(id = "1", name = "report.pdf", path = "$CONVERSATION_ID/reports/report.pdf")

        val listing = buildOfflineListing(
            currentPath = "$CONVERSATION_ID/reports",
            offlineFiles = listOf(file),
        )

        assertEquals(listOf(OfflineListingEntry.FileEntry(file)), listing)
    }

    @Test
    fun `given files saved at several depths when listing a folder then only its own nodes are listed`() {
        val directFile = offlineFile(id = "1", name = "report.pdf", path = "$CONVERSATION_ID/reports/report.pdf")
        val nestedFile = offlineFile(id = "2", name = "q1.pdf", path = "$CONVERSATION_ID/reports/2026/q1.pdf")
        val rootFile = offlineFile(id = "3", name = "notes.txt", path = "$CONVERSATION_ID/notes.txt")

        val listing = buildOfflineListing(
            currentPath = "$CONVERSATION_ID/reports",
            offlineFiles = listOf(directFile, nestedFile, rootFile),
        )

        assertEquals(
            listOf(
                OfflineListingEntry.FolderEntry(name = "2026", remotePath = "$CONVERSATION_ID/reports/2026", modifiedTime = 100L),
                OfflineListingEntry.FileEntry(directFile),
            ),
            listing,
        )
    }

    @Test
    fun `given several files in the same folder when listing then the folder is listed once with the newest time`() {
        val listing = buildOfflineListing(
            currentPath = CONVERSATION_ID,
            offlineFiles = listOf(
                offlineFile(id = "1", name = "a.pdf", path = "$CONVERSATION_ID/reports/a.pdf", modifiedAt = 100L),
                offlineFile(id = "2", name = "b.pdf", path = "$CONVERSATION_ID/reports/b.pdf", modifiedAt = 300L),
            ),
        )

        assertEquals(
            listOf(OfflineListingEntry.FolderEntry(name = "reports", remotePath = "$CONVERSATION_ID/reports", modifiedTime = 300L)),
            listing,
        )
    }

    @Test
    fun `given files of another conversation when listing a conversation then they are not listed`() {
        val listing = buildOfflineListing(
            currentPath = CONVERSATION_ID,
            offlineFiles = listOf(offlineFile(id = "1", name = "report.pdf", path = "other-conversation/report.pdf")),
        )

        assertEquals(emptyList(), listing)
    }

    @Test
    fun `given a folder listing when it has folders and files then folders come first and both are alphabetical`() {
        val notes = offlineFile(id = "1", name = "notes.txt", path = "$CONVERSATION_ID/notes.txt")
        val agenda = offlineFile(id = "2", name = "Agenda.txt", path = "$CONVERSATION_ID/Agenda.txt")
        val inReports = offlineFile(id = "3", name = "r.pdf", path = "$CONVERSATION_ID/reports/r.pdf")
        val inArchive = offlineFile(id = "4", name = "a.pdf", path = "$CONVERSATION_ID/Archive/a.pdf")

        val listing = buildOfflineListing(
            currentPath = CONVERSATION_ID,
            offlineFiles = listOf(notes, agenda, inReports, inArchive),
        )

        assertEquals(
            listOf("Archive", "reports", "Agenda.txt", "notes.txt"),
            listing.map {
                when (it) {
                    is OfflineListingEntry.FolderEntry -> it.name
                    is OfflineListingEntry.FileEntry -> it.info.name
                }
            },
        )
    }

    @Test
    fun `given a file saved before remote paths were stored when listing its conversation then it is listed at the root`() {
        val legacyFile = offlineFile(id = "1", name = "report.pdf", path = null)

        val rootListing = buildOfflineListing(currentPath = CONVERSATION_ID, offlineFiles = listOf(legacyFile))
        val folderListing = buildOfflineListing(currentPath = "$CONVERSATION_ID/reports", offlineFiles = listOf(legacyFile))

        assertEquals(listOf(OfflineListingEntry.FileEntry(legacyFile)), rootListing)
        assertEquals(emptyList(), folderListing)
    }

    @Test
    fun `given all files when listing then every offline file is listed newest first`() {
        val older = offlineFile(id = "1", name = "old.pdf", path = "$CONVERSATION_ID/reports/old.pdf", modifiedAt = 100L)
        val newer = offlineFile(id = "2", name = "new.pdf", path = "other-conversation/new.pdf", modifiedAt = 500L)

        val listing = buildOfflineListing(currentPath = null, offlineFiles = listOf(older, newer))

        assertEquals(listOf(OfflineListingEntry.FileEntry(newer), OfflineListingEntry.FileEntry(older)), listing)
    }

    private companion object {
        const val CONVERSATION_ID = "conversation-id@domain"

        fun offlineFile(
            id: String,
            name: String,
            path: String?,
            modifiedAt: Long? = 100L,
        ) = OfflineFileInfo(
            id = id,
            conversationId = CONVERSATION_ID,
            name = name,
            mimeType = "application/pdf",
            owner = "",
            localPath = "/local/$id/$name",
            size = 1024L,
            downloadedAt = 1L,
            modifiedAt = modifiedAt,
            remotePath = path,
        )
    }
}