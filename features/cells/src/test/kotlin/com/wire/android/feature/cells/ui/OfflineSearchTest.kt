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

import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfflineSearchTest {

    private val files = listOf(
        file("conv/report.pdf", modifiedAt = 2, size = 300),
        file("conv/folder/Report-final.docx", mimeType = WORD, modifiedAt = 3, size = 100),
        file("conv/photo.png", mimeType = "image/png", modifiedAt = 1, size = 200),
        file("other/report.pdf"),
    )

    @Test
    fun `given a query when searching a folder then its files and those of its sub folders matching it are found`() {
        val names = files.matching(search(query = "REPORT")).map { it.remotePath }

        assertEquals(listOf("conv/folder/Report-final.docx", "conv/report.pdf"), names)
    }

    @Test
    fun `given no query when filtering a folder then only its own files are found`() {
        val names = files.matching(search(query = "")).map { it.remotePath }

        assertEquals(listOf("conv/photo.png", "conv/report.pdf"), names)
    }

    @Test
    fun `given All files when searching then files of every conversation are found`() {
        val names = files.matching(search(currentPath = null, query = "report")).map { it.remotePath }

        assertEquals(3, names.size)
    }

    @Test
    fun `given a sorting when searching then the files are sorted accordingly`() {
        val search = search(query = "o")

        assertEquals(
            listOf("Report-final.docx", "report.pdf", "photo.png"),
            files.matching(search.copy(sortingCriteria = SortingCriteria.ByDate.NewestFirst)).map { it.name },
        )
        assertEquals(
            listOf("Report-final.docx", "photo.png", "report.pdf"),
            files.matching(search.copy(sortingCriteria = SortingCriteria.BySize.SmallestFirst)).map { it.name },
        )
        assertEquals(
            listOf("report.pdf", "Report-final.docx", "photo.png"),
            files.matching(search.copy(sortingCriteria = SortingCriteria.ByName.ZtoA)).map { it.name },
        )
    }

    private fun search(
        currentPath: String? = "conv",
        query: String,
    ) = OfflineSearch(
        currentPath = currentPath,
        query = query,
        sortingCriteria = SortingCriteria.ByName.AtoZ,
    )

    private fun file(remotePath: String, mimeType: String = "application/pdf", modifiedAt: Long = 0, size: Long = 0) =
        OfflineFileInfo(
            id = remotePath,
            conversationId = remotePath.substringBefore('/'),
            name = remotePath.substringAfterLast('/'),
            mimeType = mimeType,
            owner = "owner",
            localPath = "/local/$remotePath",
            size = size,
            downloadedAt = 0L,
            modifiedAt = modifiedAt,
            remotePath = remotePath,
        )

    private companion object {
        const val WORD = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
}
