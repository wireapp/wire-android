/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireNavigation3ContributionsTest {

    @Test
    fun givenAllMigratedContributions_whenCollectingResultTypes_thenContractIdsAreUniqueAndComplete() {
        val contractIds = WireNavigation3Contributions.resultTypes()
            .map { it.contract.id.value }

        assertEquals(
            listOf(
                "new-conversation.channel-history.custom",
                "settings.account-update",
                "user-profile.avatar-picker",
                "user-profile.connection-request-ignored",
                "conversation.completion",
                "conversation-folders.completed",
                "conversation-folders.created",
                "conversation-details.edit-name",
                "conversation-details.channel-access",
                "media.images-preview",
                "media.gallery",
                "cells.boolean",
                "cells.public-link-expiration",
                "sketch.drawing-canvas",
            ),
            contractIds,
        )
        assertEquals(contractIds.size, contractIds.toSet().size)
    }

    @Test
    fun givenAggregatorSource_whenInspectingComposition_thenContributionOrderIsDeterministic() {
        val source = appSource("navigation/runtime/WireNavigation3Contributions.kt").readText()
        val orderedMarkers = listOf(
            "AuthenticationNavigation3Contribution.entryProviderInstallers(actions, authenticationRouter)",
            "HomeNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "add(newConversationNavigation3Entries(runtime, actions))",
            "ChannelHistoryNavigation3Pilot.entryProviderInstallers(runtime)",
            "SettingsNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "DeviceE2EINavigation3Contribution.entryProviderInstallers(runtime, actions, authenticationRouter)",
            "UserProfileNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "TeamMigrationNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "ConversationNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "ConversationAuxNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "ConversationDetailsNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "MediaNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "UtilityNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "AppLockNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "CellsNavigation3Contribution.entryProviderInstallers(runtime, actions::exitCellsFlow)",
            "MeetingsNavigation3Contribution.entryProviderInstallers(runtime, actions)",
            "SketchNavigation3Contribution.entryProviderInstallers(runtime)",
        )

        val positions = orderedMarkers.map { marker ->
            source.indexOf(marker).also {
                assertTrue(it >= 0, "Missing contribution marker $marker")
            }
        }
        assertEquals(positions.sorted(), positions)
        assertTrue(source.contains("EXPECTED_INSTALLER_COUNT: Int = 19"))
    }

    @Test
    fun givenIncludedContributionSources_whenCountingTypedEntries_thenExpectedRegistrationCountIsStable() {
        val registrationCount = appContributionSources.sumOf { relativePath ->
            Regex("""wireEntry<""")
                .findAll(appSource(relativePath).readText())
                .count()
        } + featureContributionSources.sumOf { relativePath ->
            Regex("""wireEntry<""")
                .findAll(projectSource(relativePath).readText())
                .count()
        }

        assertEquals(WireNavigation3Contributions.EXPECTED_ROUTE_REGISTRATION_COUNT, registrationCount)
        assertEquals(109, registrationCount)
    }

    @Test
    fun givenAggregatorSource_whenInspectingImports_thenNoLegacyNavigationApiLeaksIntoCompositionRoot() {
        val source = appSource("navigation/runtime/WireNavigation3Contributions.kt").readText()

        listOf(
            "com.ramcosta.composedestinations",
            "androidx.navigation.NavController",
            "android.os.Bundle",
            "ScreenDestination",
            "DEFAULT_ARGS_KEY",
            "SavedStateHandle",
        ).forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Aggregator must not reference $forbidden")
        }
        assertTrue(source.contains("interface WireNavigation3CompositeActions"))
        assertTrue(source.contains("ConversationEntryNavigation3Actions"))
        assertTrue(source.contains("MeetingsNavigation3Actions"))
        assertTrue(source.contains("override val topLevel: HomeTopLevelNavigation3Actions"))
        assertTrue(source.contains("override val settings: SettingsNavigation3Actions"))
        assertFalse(source.contains("WhatsNewNavigationActions"))
        assertFalse(source.contains("override val meetings:"))
        assertFalse(source.contains("override val whatsNew:"))
    }

    private fun appSource(relativePath: String): File {
        return projectSource("app/src/main/kotlin/com/wire/android/$relativePath")
    }

    private fun projectSource(relativePath: String): File {
        val projectDir = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(projectDir, relativePath).also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }

    private companion object {
        val appContributionSources = listOf(
            "navigation/routes/auth/AuthenticationNavigation3Entries.kt",
            "navigation/routes/auth/CreateAccountNavigation3Entries.kt",
            "ui/home/HomeNavigation3Entry.kt",
            "ui/home/newconversation/NewConversationNavigation3Entries.kt",
            "ui/home/newconversation/channelhistory/ChannelHistoryNavigation3Entries.kt",
            "ui/home/settings/SettingsNavigation3Entries.kt",
            "ui/home/settings/SettingsAccountNavigation3Entries.kt",
            "ui/settings/devices/DeviceE2EINavigation3Entries.kt",
            "ui/userprofile/UserProfileNavigation3Entries.kt",
            "ui/userprofile/teammigration/TeamMigrationNavigation3Entries.kt",
            "ui/home/conversations/ConversationNavigation3Entries.kt",
            "ui/home/conversations/ConversationAuxNavigation3Entries.kt",
            "ui/home/conversations/details/ConversationDetailsNavigation3Entries.kt",
            "navigation/routes/media/MediaNavigation3Entries.kt",
            "navigation/routes/utility/UtilityNavigation3Entries.kt",
            "ui/home/appLock/AppLockNavigation3Entries.kt",
        )
        val featureContributionSources = listOf(
            "features/meetings/src/main/java/com/wire/android/feature/meetings/ui/create/MeetingsNavigation3Entries.kt",
            "features/sketch/src/main/java/com/wire/android/feature/sketch/navigation/SketchNavigation3Entries.kt",
            "features/cells/src/main/java/com/wire/android/feature/cells/navigation/CellsNavigation3Entries.kt",
        )
    }
}
