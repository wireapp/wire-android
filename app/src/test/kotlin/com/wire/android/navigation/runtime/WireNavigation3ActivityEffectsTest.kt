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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireNavigation3ActivityEffectsTest {

    private val links = WireNavigation3ExternalLinks(
        support = "https://wire.test/support",
        teamManagement = "https://wire.test/teams",
        teamPlan = "https://wire.test/plans",
        termsOfUse = "https://wire.test/legal",
        wireWebsite = "https://wire.test",
        privacyPolicy = "https://wire.test/privacy",
        reportMisuse = "https://wire.test/report",
        welcomeAndroid = "https://wire.test/welcome-android",
        androidReleaseNotes = "https://wire.test/android-releases",
    )

    @Test
    fun givenUrlExternalDestinations_whenResolvingEffects_thenEverySemanticTargetUsesItsOwnUrl() {
        val expected = mapOf(
            WireNavigation3ExternalIntent.SUPPORT to links.support,
            WireNavigation3ExternalIntent.TEAM_MANAGEMENT to links.teamManagement,
            WireNavigation3ExternalIntent.TEAM_PLAN to links.teamPlan,
            WireNavigation3ExternalIntent.TERMS_OF_USE to links.termsOfUse,
            WireNavigation3ExternalIntent.WIRE_WEBSITE to links.wireWebsite,
            WireNavigation3ExternalIntent.PRIVACY_POLICY to links.privacyPolicy,
            WireNavigation3ExternalIntent.REPORT_MISUSE to links.reportMisuse,
            WireNavigation3ExternalIntent.WELCOME_ANDROID to links.welcomeAndroid,
            WireNavigation3ExternalIntent.ANDROID_RELEASE_NOTES to links.androidReleaseNotes,
        )

        expected.forEach { (destination, url) ->
            assertEquals(
                WireNavigation3PlatformEffect.CustomTab(url),
                resolveWireNavigation3PlatformEffect(destination, links),
                destination.name,
            )
        }
    }

    @Test
    fun givenFeedbackDestination_whenResolvingEffect_thenEmailComposerIsSelected() {
        assertSame(
            WireNavigation3PlatformEffect.GiveFeedbackEmail,
            resolveWireNavigation3PlatformEffect(
                WireNavigation3ExternalIntent.GIVE_FEEDBACK,
                links,
            ),
        )
    }

    @Test
    fun givenBlankTeamManagementUrl_whenResolvingEffect_thenNoPlatformActionIsProduced() {
        assertSame(
            WireNavigation3PlatformEffect.None,
            resolveWireNavigation3PlatformEffect(
                WireNavigation3ExternalIntent.TEAM_MANAGEMENT,
                links.copy(teamManagement = ""),
            ),
        )
    }

    @Test
    fun givenActivityEffects_whenInspectingSource_thenNoLegacyNavigationTypeLeaksIntoTheHelper() {
        val source = sourceFile().readText()

        listOf(
            "WireNavigation3ActivityCallbacks",
            "WireNavigation3ExternalIntent",
            "CustomTabsHelper",
            "SupportUrlResolver",
            "WireBackStackMode.UPDATE_EXISTING",
            "Intent.FLAG_ACTIVITY_CLEAR_TASK",
            "Intent.ACTION_SENDTO",
        ).forEach { expected -> assertTrue(expected in source, expected) }
        listOf(
            "com.ramcosta",
            "Direction",
            "NavController",
            "com.wire.android.navigation.NavigationCommand",
            "generated.",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    private fun sourceFile(): File {
        val relative =
            "src/main/kotlin/com/wire/android/navigation/runtime/WireNavigation3ActivityEffects.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
