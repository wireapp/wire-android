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

package com.wire.android.ui.common

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityIndicatorOwnershipSourceTest {

    @Test
    fun neutralIndicatorsAreOwnedByCoreUiCommonWhileAppKeepsOnlyItsPreviewAndKaliumAdapters() {
        val legalHoldCore = sourceFile("core/ui-common/src/main/kotlin/com/wire/android/ui/common/LegalHoldIndicator.kt")
        val verifiedIconsCore = sourceFile("core/ui-common/src/main/kotlin/com/wire/android/ui/common/VerifiedIcons.kt")
        val legalHoldApp = sourceFile("app/src/main/kotlin/com/wire/android/ui/common/LegalHoldIndicator.kt")
        val verifiedIconsApp = sourceFile("app/src/main/kotlin/com/wire/android/ui/common/VerifiedIcons.kt")

        assertTrue(legalHoldCore.contains("fun LegalHoldIndicator("))
        assertTrue(verifiedIconsCore.contains("fun ProteusVerifiedIcon("))
        assertTrue(verifiedIconsCore.contains("fun MLSVerifiedIcon("))
        assertFalse(legalHoldCore.contains("com.wire.kalium"))
        assertFalse(verifiedIconsCore.contains("com.wire.kalium"))

        assertTrue(legalHoldApp.contains("fun PreviewLegalHoldIndicator("))
        assertFalse(legalHoldApp.contains("fun LegalHoldIndicator("))
        assertTrue(verifiedIconsApp.contains("fun RowScope.ConversationVerificationIcons("))
        assertTrue(verifiedIconsApp.contains("fun RowScope.MLSVerificationIcon("))
        assertTrue(verifiedIconsApp.contains("fun MLSRevokedIcon("))
        assertTrue(verifiedIconsApp.contains("fun MLSNotVerifiedIcon("))
        assertFalse(verifiedIconsApp.contains("fun ProteusVerifiedIcon("))
        assertFalse(verifiedIconsApp.contains("fun MLSVerifiedIcon("))
    }

    @Test
    fun sharedSecurityResourcesAreOwnedByCoreUiCommonWithTheirFullLocalizedCoverage() {
        val appResources = resourceDirectory("app")
        val coreResources = resourceDirectory("core/ui-common")

        securityDrawables.forEach { drawable ->
            assertFalse(File(appResources, drawable).isFile, "$drawable must not remain in :app resources.")
            assertTrue(File(coreResources, drawable).isFile, "$drawable must be owned by :core:ui-common.")
        }

        assertTrue(qualifiersContaining(appResources, "label_client_verified").isEmpty())
        assertEquals(verifiedLabelValues.keys, qualifiersContaining(coreResources, "label_client_verified"))
        verifiedLabelValues.forEach { (qualifier, expectedValue) ->
            assertEquals(expectedValue, stringDefinition(coreResources, qualifier, "label_client_verified"))
        }
    }

    @Test
    fun directAppConsumersUseTheCoreUiCommonResourceNamespace() {
        assertTrue(
            sourceFile("app/src/main/kotlin/com/wire/android/ui/home/E2EIDialogs.kt")
                .contains("commonR.drawable.ic_certificate_valid_mls"),
        )
        assertTrue(
            sourceFile("app/src/main/kotlin/com/wire/android/ui/settings/devices/EndToEndIdentityCertificateItem.kt")
                .contains("commonR.drawable.ic_certificate_valid_mls"),
        )
        val deviceDetails = sourceFile("app/src/main/kotlin/com/wire/android/ui/settings/devices/DeviceDetailsScreen.kt")
        assertTrue(deviceDetails.contains("commonR.string.label_client_verified"))
        val systemMessage = sourceFile("app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/SystemMessageItem.kt")
        assertTrue(systemMessage.contains("commonR.drawable.ic_certificate_valid_mls"))
        assertTrue(systemMessage.contains("commonR.drawable.ic_certificate_valid_proteus"))
        assertTrue(systemMessage.contains("commonR.drawable.ic_legal_hold"))
    }

    private fun qualifiersContaining(resourceDirectory: File, resourceName: String): Set<String> =
        resourceDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .filter { it.readText().contains("name=\"$resourceName\"") }
            .map { requireNotNull(it.parentFile).name }
            .toSet()

    private fun stringDefinition(resourceDirectory: File, qualifier: String, resourceName: String): String {
        val file = File(resourceDirectory, "$qualifier/security_indicator_strings.xml")
        assertTrue(file.isFile, "Missing ${file.path}")
        val match = Regex("""<string\s+name=\"$resourceName\"[^>]*>(.*?)</string>""").find(file.readText())

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
        val securityDrawables = setOf(
            "drawable/ic_legal_hold.xml",
            "drawable/ic_certificate_valid_mls.xml",
            "drawable-night/ic_certificate_valid_mls.xml",
            "drawable/ic_certificate_valid_proteus.xml",
            "drawable-night/ic_certificate_valid_proteus.xml",
        )
        val verifiedLabelValues = mapOf(
            "values" to "Verified",
            "values-de" to "Überprüft",
            "values-es" to "Verificado",
            "values-et" to "Kinnitatud",
            "values-hu" to "Ellenőrizve",
            "values-it" to "Verificato",
            "values-pl" to "Zweryfikowane",
            "values-pt" to "Verificado",
            "values-ru" to "Верифицирован",
            "values-si" to "සත්‍යාපිතයි",
        )
    }
}
