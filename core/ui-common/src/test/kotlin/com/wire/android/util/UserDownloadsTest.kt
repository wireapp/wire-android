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

package com.wire.android.util

import android.content.Context
import com.wire.kalium.logic.data.user.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UserDownloadsTest {

    @TempDir
    lateinit var root: File

    @Test
    fun givenUserId_whenResolvingDownloadsDirectory_thenItIsTheCellsFolderOfDomainAndValueUnderTheRoot() {
        assertEquals(File(root, "wire.com/alice/cells"), root.userDownloadsDir(ALICE))
    }

    @Test
    fun givenUuidValue_whenResolvingDownloadsDirectory_thenItIsUnchanged() {
        val uuid = "1b4e28ba-2fa1-11d2-883f-0016d3cca427"

        assertEquals(File(root, "wire.com/$uuid/cells"), root.userDownloadsDir(UserId(uuid, "wire.com")))
    }

    @Test
    fun givenIdsThatSanitizeToTheSameName_whenResolvingDownloadsDirectory_thenTheFoldersDiffer() {
        assertNotEquals(
            root.userDownloadsDir(UserId("alice", "host:8080")),
            root.userDownloadsDir(UserId("alice", "host_8080"))
        )
        assertNotEquals(
            root.userDownloadsDir(UserId("a/b", "wire.com")),
            root.userDownloadsDir(UserId("a_b", "wire.com"))
        )
    }

    @Test
    fun givenUnsafeIds_whenResolvingDownloadsDirectory_thenItIsThreeSingleSegmentLevelsInsideTheRoot() {
        listOf("..", ".", "", "a/b", "host:8080").forEach { unsafe ->
            val relative = root.userDownloadsDir(UserId(unsafe, unsafe)).relativeTo(root)
            val segments = relative.path.split(File.separatorChar)

            assertEquals(3, segments.size, unsafe)
            assertEquals("cells", segments[2], unsafe)
            segments.forEach { assertFalse(it.isEmpty() || it == "." || it == "..", unsafe) }
            assertEquals(File(root, relative.path).canonicalFile, root.userDownloadsDir(UserId(unsafe, unsafe)).canonicalFile)
        }
    }

    @Test
    fun givenKaliumFileInTheUserFolder_whenDeletingUserDownloads_thenOnlyTheCellsFolderIsRemoved() {
        val filesDirectory = File(root, "files").apply(File::mkdirs)
        val assetFile = file(filesDirectory, "wire.com/alice/asset.bin")
        val cellsFile = file(filesDirectory.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val context = mockk<Context> {
            every { getExternalFilesDir(null) } returns null
            every { filesDir } returns filesDirectory
        }

        context.deleteUserDownloads(ALICE)

        assertFalse(cellsFile.exists())
        assertFalse(filesDirectory.userDownloadsDir(ALICE).exists())
        assertTrue(assetFile.exists())
    }

    @Test
    fun givenNothingElseInTheUserFolders_whenDeletingUserDownloads_thenEmptyUserAndDomainFoldersAreRemoved() {
        file(root.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val context = contextWithExternalDirectory(root)

        context.deleteUserDownloads(ALICE)

        assertFalse(File(root, "wire.com").exists())
        assertTrue(root.exists())
    }

    @Test
    fun givenAnotherUserInTheDomain_whenDeletingUserDownloads_thenTheDomainFolderStays() {
        val bobFile = file(root.userDownloadsDir(BOB), "conversation/uuid/report.pdf")
        file(root.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")

        contextWithExternalDirectory(root).deleteUserDownloads(ALICE)

        assertFalse(File(root, "wire.com/alice").exists())
        assertTrue(bobFile.exists())
    }

    @Test
    fun givenFilesOfTwoUsers_whenDeletingOneUsersDownloads_thenOnlyThatUsersFilesAreRemoved() {
        val aliceFile = file(root.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val bobFile = file(root.userDownloadsDir(BOB), "conversation/uuid/report.pdf")
        val legacyFile = file(root, "conversation/uuid/old.pdf")
        val context = contextWithExternalDirectory(root)

        context.deleteUserDownloads(ALICE)

        assertFalse(aliceFile.exists())
        assertFalse(root.userDownloadsDir(ALICE).exists())
        assertTrue(bobFile.exists())
        assertTrue(legacyFile.exists())
    }

    @Test
    fun givenNoExternalDirectory_whenDeletingUserDownloads_thenTheFilesDirectoryIsUsed() {
        val filesDirectory = File(root, "files").apply(File::mkdirs)
        val aliceFile = file(filesDirectory.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val context = mockk<Context> {
            every { getExternalFilesDir(null) } returns null
            every { filesDir } returns filesDirectory
        }

        context.deleteUserDownloads(ALICE)

        assertFalse(aliceFile.exists())
    }

    @Test
    fun givenUserWithoutDownloads_whenDeletingUserDownloads_thenNothingFails() {
        contextWithExternalDirectory(root).deleteUserDownloads(ALICE)

        assertTrue(root.exists())
    }

    @Test
    fun givenFilesInBothStorageRoots_whenDeletingUserDownloads_thenBothAreRemoved() {
        val filesDirectory = File(root, "files").apply(File::mkdirs)
        val externalDirectory = File(root, "external").apply(File::mkdirs)
        val externalFile = file(externalDirectory.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val internalFile = file(filesDirectory.userDownloadsDir(ALICE), "conversation/uuid/report.pdf")
        val context = mockk<Context> {
            every { getExternalFilesDir(null) } returns externalDirectory
            every { filesDir } returns filesDirectory
        }

        context.deleteUserDownloads(ALICE)

        assertFalse(externalFile.exists())
        assertFalse(internalFile.exists())
    }

    @Test
    fun givenUserIdWithoutDomain_whenResolvingDownloadsDirectory_thenAPlaceholderFolderIsUsed() {
        val relative = root.userDownloadsDir(UserId("alice", "")).relativeTo(root).path

        assertTrue(Regex("_-[0-9a-f]{12}/alice/cells").matches(relative), relative)
    }

    @Test
    fun givenCharactersThatAreNotSafeInFolderNames_whenResolvingDownloadsDirectory_thenTheyAreReplacedAndHashed() {
        val relative = root.userDownloadsDir(UserId("a/b", "host:8080")).relativeTo(root).path

        assertTrue(Regex("host_8080-[0-9a-f]{12}/a_b-[0-9a-f]{12}/cells").matches(relative), relative)
    }

    @Test
    fun givenDotSegments_whenResolvingDownloadsDirectory_thenEachGetsADistinctPlaceholder() {
        val folders = listOf("..", ".", "").map { root.userDownloadsDir(UserId(it, it)) }

        assertEquals(3, folders.toSet().size)
    }

    @Test
    fun givenUserIdThatPointsAtTheParent_whenDeletingUserDownloads_thenNothingOutsideThatUserIsRemoved() {
        val bobFile = file(root.userDownloadsDir(BOB), "conversation/uuid/report.pdf")
        val legacyFile = file(root, "conversation/uuid/old.pdf")
        val context = contextWithExternalDirectory(root)

        context.deleteUserDownloads(UserId("..", ".."))
        context.deleteUserDownloads(UserId("", ""))

        assertTrue(bobFile.exists())
        assertTrue(legacyFile.exists())
        assertTrue(root.exists())
    }

    private fun contextWithExternalDirectory(directory: File) = mockk<Context> {
        every { getExternalFilesDir(null) } returns directory
        every { filesDir } returns File(directory, "internal-files-not-used")
    }

    private fun file(directory: File, relativePath: String) = File(directory, relativePath).apply {
        parentFile?.mkdirs()
        writeText("content")
    }

    private companion object {
        val ALICE = UserId("alice", "wire.com")
        val BOB = UserId("bob", "wire.com")
    }
}
