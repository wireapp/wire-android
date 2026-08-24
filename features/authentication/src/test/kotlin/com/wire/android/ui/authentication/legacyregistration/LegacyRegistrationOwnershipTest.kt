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

    private companion object {
        val featureSources = listOf(
            "com/wire/android/ui/authentication/legacyregistration/selector/LegacyRegistrationSelectorViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsGateway.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsState.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/details/LegacyRegistrationDetailsContent.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeGateway.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeState.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeViewModel.kt",
            "com/wire/android/ui/authentication/legacyregistration/code/LegacyRegistrationCodeContent.kt",
        )
    }
}
