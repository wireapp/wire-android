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

package com.wire.android.ui.home.conversations

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QualifiedIdParcelerOwnershipSourceTest {

    @Test
    fun qualifiedIdParcelerIsCoreOwnedWhileExistingConsumersKeepItsFqn() {
        val parceler = sourceFile(
            "core/ui-common/src/main/kotlin/com/wire/android/ui/home/conversations/QualifiedIdParceler.kt",
        )
        val conversationNavArgs = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/ConversationNavArgs.kt",
        )
        val promoteAdminNavArgs = sourceFile(
            "app/src/main/kotlin/com/wire/android/ui/home/conversations/promoteadmin/PromoteAdminNavArgs.kt",
        )

        assertTrue(parceler.contains("package com.wire.android.ui.home.conversations"))
        assertTrue(parceler.contains("object QualifiedIdParceler : Parceler<QualifiedID>"))
        assertTrue(parceler.contains("parcel.readString().orEmpty()"))
        assertTrue(parceler.contains("parcel.writeString(this.value + \"@\" + this.domain)"))

        assertFalse(conversationNavArgs.contains("object QualifiedIdParceler"))
        assertTrue(conversationNavArgs.contains("@TypeParceler<ConversationId, QualifiedIdParceler>()"))
        assertTrue(
            promoteAdminNavArgs.contains(
                "import com.wire.android.ui.home.conversations.QualifiedIdParceler",
            ),
        )
        assertTrue(promoteAdminNavArgs.contains("@TypeParceler<QualifiedID, QualifiedIdParceler>()"))
    }

    private fun sourceFile(relativePath: String): String =
        File(repositoryRoot(), relativePath).also { file ->
            assertTrue(file.isFile, "Missing ${file.path}")
        }.readText()

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
}
