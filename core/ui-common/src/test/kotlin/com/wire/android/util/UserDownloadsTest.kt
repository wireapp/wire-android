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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UserDownloadsTest {

    @TempDir
    lateinit var root: File

    @Test
    fun givenUserId_whenResolvingDownloadsDirectory_thenItIsDomainAndValueUnderTheRoot() {
        assertEquals(File(root, "wire.com/alice"), root.userDownloadsDir(ALICE))
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
        assertEquals(File(root, "_/alice"), root.userDownloadsDir(UserId("alice", "")))
    }

    @Test
    fun givenCharactersThatAreNotSafeInFolderNames_whenResolvingDownloadsDirectory_thenTheyAreReplaced() {
        assertEquals(File(root, "host_8080/a_b"), root.userDownloadsDir(UserId("a/b", "host:8080")))
    }

    @Test
    fun givenDotSegments_whenResolvingDownloadsDirectory_thenTheDirectoryStaysTwoLevelsInsideTheRoot() {
        assertEquals(File(root, "_/_"), root.userDownloadsDir(UserId("..", "..")))
        assertEquals(File(root, "_/_"), root.userDownloadsDir(UserId(".", ".")))
        assertEquals(File(root, "_/_"), root.userDownloadsDir(UserId("", "")))
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
