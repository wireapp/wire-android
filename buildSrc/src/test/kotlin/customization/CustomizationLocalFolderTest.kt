/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package customization

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class CustomizationLocalFolderTest {

    @Suppress("VulnerableCodeUsages") // We don't write sensitive info, and this is patched in newer JDKs
    @get:Rule
    val rootDir: TemporaryFolder = TemporaryFolder()

    @Test
    fun givenLocalFolderProperty_whenGettingBuildInfo_thenCopiesFolderAndUsesIt() {
        File(rootDir.root, "default.json").writeText(
            """{"flavors":{"dev":{}},"foo":"default"}"""
        )
        val sourceDir = File(rootDir.root, "mysource")
        sourceDir.mkdirs()
        File(sourceDir, "custom-reloaded.json").writeText(
            """{"flavors":{"dev":{}},"foo":"custom"}"""
        )

        File(rootDir.root, "local.properties").writeText(
            """
            CUSTOM_LOCAL_FOLDER=mysource
            """.trimIndent()
        )

        val result = Customization.getBuildtimeConfiguration(rootDir.root)

        assertEquals("custom", result.flavorSettings.flavorMap["dev"]!!["foo"])
        assertTrue(File(rootDir.root, "custom/custom-reloaded.json").exists())
    }

    @Test
    fun givenBothRepositoryAndLocalFolderSet_whenGettingBuildInfo_thenThrows() {
        File(rootDir.root, "default.json").writeText(
            """{"flavors":{"dev":{}},"foo":"default"}"""
        )
        File(rootDir.root, "local.properties").writeText(
            """
            CUSTOM_LOCAL_FOLDER=mysource
            CUSTOM_REPOSITORY=git@example.com:foo.git
            """.trimIndent()
        )

        assertFailsWith<IllegalArgumentException> {
            Customization.getBuildtimeConfiguration(rootDir.root)
        }
    }
}
