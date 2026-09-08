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

package com.wire.android.ui.home

import com.wire.android.ui.home.whatsnew.WhatsNewItem
import com.wire.android.ui.home.whatsnew.WhatsNewNavigationTarget
import com.wire.android.ui.home.whatsnew.toNavigationTarget
import com.wire.android.util.ui.UIText
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeTopLevelNavigation3Test {

    @Test
    fun givenWhatsNewItems_whenMapped_thenOnlySemanticTargetsCrossTheHomeBoundary() {
        assertEquals(
            WhatsNewNavigationTarget.Welcome,
            WhatsNewItem.WelcomeToNewAndroidApp.toNavigationTarget(),
        )
        assertEquals(
            WhatsNewNavigationTarget.AllAndroidReleaseNotes,
            WhatsNewItem.AllAndroidReleaseNotes().toNavigationTarget(),
        )

        val releaseNote = WhatsNewItem.AndroidReleaseNotes(
            id = "release",
            title = UIText.DynamicString("Release"),
            url = "https://wire.com/release",
        )
        assertEquals(
            WhatsNewNavigationTarget.ExternalReleaseNote("https://wire.com/release"),
            releaseNote.toNavigationTarget(),
        )
    }

    @Test
    fun givenNavigation3TopLevelRenderer_whenInspectingSource_thenItDoesNotHideLegacyNavigation() {
        val source = sourceFile().readText()

        forbiddenLegacyTokens.forEach { token ->
            assertFalse(source.contains(token), "Top-level renderer still references $token")
        }
        renderedDestinations.forEach { destination ->
            assertTrue(
                source.contains("HomeTopLevelDestination.$destination"),
                "Top-level renderer does not handle $destination",
            )
        }
    }

    @Test
    fun givenHomeTopLevelActions_whenDefiningChildNavigation_thenEveryChildUsesAnOwnedContract() {
        val contract = sourceFile().readText()
            .substringAfter("internal interface HomeTopLevelNavigation3Actions {")
            .substringBefore("\n}")

        listOf(
            "val conversationList: ConversationListNavigationActions",
            "val settings: SettingsNavigation3Actions",
            "val cells: AllFilesNavigationActions",
            "val whatsNew: WhatsNewNavigationActions",
            "val meetings: MeetingsHomeNavigationActions",
        ).forEach { property ->
            assertTrue(contract.contains(property), "Missing top-level child contract: $property")
        }
        assertFalse(contract.contains("fun "), "Feature-specific actions must not be flattened into Home")
    }

    @Test
    fun givenHomeShellActions_whenDefiningNavigation_thenChildNavigationIsOnlyExposedThroughTopLevel() {
        val contract = homeEntrySourceFile().readText()
            .substringAfter("internal interface HomeNavigation3Actions {")
            .substringBefore("\n}")
        val declarations = contract.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        assertEquals(
            listOf(
                "val topLevel: HomeTopLevelNavigation3Actions",
                "fun onRequirement(requirement: HomeRequirement)",
                "fun openSelfProfile()",
                "fun openExternal(destination: HomeExternalDestination)",
            ),
            declarations,
            "Home child navigation must be exposed only through the top-level aggregate",
        )
    }

    private fun sourceFile(): File {
        val relative = "src/main/kotlin/com/wire/android/ui/home/HomeNavigation3TopLevelContent.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }

    private fun homeEntrySourceFile(): File {
        val relative = "src/main/kotlin/com/wire/android/ui/home/HomeNavigation3Entry.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }

    private companion object {
        val forbiddenLegacyTokens = listOf(
            "DestinationsNavHost",
            "generated.",
            "NavController",
            "NavigationCommand",
            "WireNavigator",
        )
        val renderedDestinations = listOf(
            "SETTINGS",
            "VAULT",
            "ARCHIVE",
            "WHATS_NEW",
            "CELLS",
            "MEETINGS",
        )
    }
}
