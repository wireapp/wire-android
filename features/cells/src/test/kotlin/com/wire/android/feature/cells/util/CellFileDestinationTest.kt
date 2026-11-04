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
package com.wire.android.feature.cells.util

import com.wire.kalium.logic.data.user.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CellFileDestinationTest {

    @TempDir
    lateinit var root: File

    private val fileHelper = mockk<FileHelper>()

    private fun destination(userId: UserId, conversationId: String?, uuid: String, name: String): File {
        every { fileHelper.getExternalFilesDir() } returns root
        return fileHelper.cellFileDestination(userId, conversationId, uuid, name)
    }

    @Test
    fun givenConversation_whenResolvingDestination_thenItIsUnderTheUserFolder() {
        val result = destination(ALICE, "conversation@wire.com", "uuid-1", "report.pdf")

        assertEquals(File(root, "wire.com/alice/cells/conversation@wire.com/uuid-1/report.pdf"), result)
        assertTrue(result.parentFile!!.isDirectory)
    }

    @Test
    fun givenNoConversation_whenResolvingDestination_thenTheUuidFolderIsDirectlyUnderTheUserFolder() {
        assertEquals(File(root, "wire.com/alice/cells/uuid-1/report.pdf"), destination(ALICE, null, "uuid-1", "report.pdf"))
        assertEquals(File(root, "wire.com/alice/cells/uuid-1/report.pdf"), destination(ALICE, "", "uuid-1", "report.pdf"))
    }

    @Test
    fun givenTwoUsersWithTheSameConversationAndFile_whenResolvingDestination_thenTheFilesDiffer() {
        val alice = destination(ALICE, "conversation", "uuid-1", "report.pdf")
        val bob = destination(BOB, "conversation", "uuid-1", "report.pdf")

        assertNotEquals(alice, bob)
    }

    @Test
    fun givenNameWithSpacesAndBrackets_whenResolvingDestination_thenTheNameIsKept() {
        val result = destination(ALICE, "conversation", "uuid-1", "Quarterly report (final) ä.pdf")

        assertEquals("Quarterly report (final) ä.pdf", result.name)
    }

    @Test
    fun givenServerValuesWithPathParts_whenResolvingDestination_thenTheFileStaysInsideTheUserFolder() {
        val userFolder = File(root, "wire.com/alice/cells").canonicalFile.toPath()

        val results = listOf(
            destination(ALICE, "../../wire.com/bob", "uuid-1", "report.pdf"),
            destination(ALICE, "conversation", "..", "report.pdf"),
            destination(ALICE, "conversation", "uuid-1", "../../bob/report.pdf"),
            destination(ALICE, "conversation", "uuid-1", ".."),
            destination(ALICE, "/etc", "uuid-1", "a\\b.pdf"),
        )

        results.forEach { assertTrue(it.canonicalFile.toPath().startsWith(userFolder), "$it is outside the user folder") }
    }

    private companion object {
        val ALICE = UserId("alice", "wire.com")
        val BOB = UserId("bob", "wire.com")
    }
}
