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

package com.wire.android.ui.home.newconversation.channelaccess

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChannelAccessOwnershipSourceTest {

    @Test
    fun channelAccessPresentationTypesAreOwnedByCoreUiCommon() {
        val channelAccessType = sourceFile(
            "core/ui-common/src/main/kotlin/com/wire/android/ui/home/newconversation/channelaccess/ChannelAccessType.kt",
        )
        val channelAddPermissionType = sourceFile(
            "core/ui-common/src/main/kotlin/com/wire/android/ui/home/newconversation/channelaccess/ChannelAddPermissionType.kt",
        )

        assertTrue(channelAccessType.contains("package com.wire.android.ui.home.newconversation.channelaccess"))
        assertTrue(channelAccessType.contains("import com.wire.android.ui.common.R"))
        assertTrue(channelAddPermissionType.contains("package com.wire.android.ui.home.newconversation.channelaccess"))
        assertTrue(channelAddPermissionType.contains("import com.wire.android.ui.common.R"))
        assertFalse(
            File(
                repositoryRoot(),
                "app/src/main/kotlin/com/wire/android/ui/home/newconversation/channelaccess/ChannelAccessType.kt",
            ).exists(),
        )
        assertFalse(
            File(
                repositoryRoot(),
                "app/src/main/kotlin/com/wire/android/ui/home/newconversation/channelaccess/ChannelAddPermissionType.kt",
            ).exists(),
        )
    }

    @Test
    fun channelAccessLabelsAreOwnedByCoreUiCommonWithTheirFullLocalizedCoverage() {
        channelAccessLabels.forEach { (resourceName, expectedValues) ->
            assertResourceOwnership(resourceName, expectedValues)
        }
    }

    private fun assertResourceOwnership(resourceName: String, expectedValues: Map<String, String>) {
        val appQualifiers = qualifiersContaining(resourceDirectory("app"), resourceName)
        val coreQualifiers = qualifiersContaining(resourceDirectory("core/ui-common"), resourceName)

        assertTrue(appQualifiers.isEmpty(), "$resourceName must not remain in :app resources.")
        assertEquals(expectedValues.keys, coreQualifiers, "$resourceName must preserve its qualifier coverage.")
        expectedValues.forEach { (qualifier, expectedValue) ->
            assertEquals(expectedValue, stringDefinition(resourceDirectory("core/ui-common"), qualifier, resourceName))
        }
    }

    private fun qualifiersContaining(resourceDirectory: File, resourceName: String): Set<String> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .filter { it.readText().contains("name=\"$resourceName\"") }
            .map { requireNotNull(it.parentFile).name }
            .toSet()

    private fun stringDefinition(resourceDirectory: File, qualifier: String, resourceName: String): String {
        val file = File(resourceDirectory, "$qualifier/strings.xml")
        assertTrue(file.isFile, "Missing ${file.path}")
        val match = Regex("""<string\s+name="$resourceName"[^>]*>(.*?)</string>""").find(file.readText())

        assertTrue(match != null, "Missing $resourceName in ${file.path}")
        return requireNotNull(match).groupValues[1]
    }

    private fun sourceFile(relativePath: String): String = File(repositoryRoot(), relativePath).also { file ->
        assertTrue(file.isFile, "Missing ${file.path}")
    }.readText()

    private fun resourceDirectory(module: String): File = File(repositoryRoot(), "$module/src/main/res")

    private fun repositoryRoot(): File =
        generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        val channelAccessLabels = mapOf(
            "channel_private_label" to mapOf(
                "values" to "Private",
                "values-de" to "Privat",
                "values-hu" to "Magán",
                "values-pt" to "Privado",
                "values-ru" to "Приватный",
                "values-si" to "පුද්ගලික",
            ),
            "channel_public_label" to mapOf(
                "values" to "Public",
                "values-de" to "Öffentlich",
                "values-hu" to "Nyilvános",
                "values-pt" to "Público",
                "values-ru" to "Публичный",
                "values-si" to "පොදු",
            ),
            "channel_add_permission_admin_label" to mapOf(
                "values" to "Admins",
                "values-de" to "Admins",
                "values-hu" to "Adminok",
                "values-pt" to "Administradores",
                "values-ru" to "Администраторы",
                "values-si" to "පරිපාලකයින්",
            ),
            "channel_add_permission_admin_members_label" to mapOf(
                "values" to "Admins and members",
                "values-de" to "Admins und Mitglieder",
                "values-hu" to "Adminok és tagok",
                "values-pt" to "Administradores e membros",
                "values-ru" to "Администраторы и члены",
                "values-si" to "පරිපාලකයින් සහ සාමාජිකයින්",
            ),
        )
    }
}
