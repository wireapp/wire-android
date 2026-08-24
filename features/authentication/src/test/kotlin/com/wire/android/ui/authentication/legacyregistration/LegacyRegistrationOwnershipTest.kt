/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyRegistrationOwnershipTest {
    @Test
    fun givenLegacyRegistrationFeature_whenInspectingSources_thenFeatureStaysHostAgnostic() {
        featureSources.forEach { source ->
            val text = Files.readString(Path.of("src/main/kotlin").resolve(source))
            listOf("com.wire.kalium", "BuildConfig", "datastore", "dev.zacsweers.metro", "com.wire.android.R").forEach {
                assertFalse(it in text, "$source leaked host dependency $it")
            }
        }
    }

    @Test
    fun givenLegacyRegistrationFeature_whenInspectingSources_thenItOwnsItsStateMachines() {
        assertTrue(featureSources.count { it.endsWith("ViewModel.kt") } >= 3)
    }

    @Test
    fun givenLegacyRegistrationExtraction_whenInspectingHost_thenCompatibilityStatesAreDeleted() {
        val root = repositoryRoot()
        listOf(
            "app/src/main/kotlin/com/wire/android/ui/registration/details/CreateAccountDataDetailViewState.kt",
            "app/src/main/kotlin/com/wire/android/ui/registration/code/CreateAccountVerificationCodeViewState.kt",
        ).forEach { path ->
            assertFalse(Files.exists(root.resolve(path)), "Stale app compatibility state: $path")
        }
    }

    @Test
    fun givenLegacyRegistrationResources_whenInspectingOwners_thenFeatureContainsMigratedStrings() {
        val root = repositoryRoot()
        val featureResource = root.resolve("features/authentication/src/main/res/values/legacy_registration.xml")
        val content = Files.readString(featureResource)
        listOf(
            "create_account_selector_title",
            "create_account_email_backlink_to_team_label",
            "create_account_details_name_placeholder",
            "create_account_code_error_title",
        ).forEach { name ->
            assertTrue(content.contains("name=\"$name\""), "Missing feature resource: $name")
        }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val featureSources = listOf(
            "com/wire/android/ui/authentication/legacyregistration/selector/LegacyRegistrationSelectorViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsGateway.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsState.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsContent.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsForm.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationPasswordFields.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeGateway.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeState.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeContent.kt",
            "com/wire/android/ui/authentication/legacyregistration/selector/LegacyRegistrationSelectorContent.kt",
            "com/wire/android/ui/authentication/legacyregistration/selector/LegacyRegistrationSelectorCard.kt",
        )
    }
}
