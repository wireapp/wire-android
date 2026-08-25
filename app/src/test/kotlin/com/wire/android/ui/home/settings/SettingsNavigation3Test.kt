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

package com.wire.android.ui.home.settings

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SettingsNavigation3Test {

    private val sessionId = WireSessionId("user", "wire.example")

    @Test
    fun givenSettingsRoutes_whenSerializedAndRestored_thenSessionAndEntryIdentityArePreserved() {
        assertSerializationRoundTrip(
            SettingsRoute(sessionId, WireNavEntryId("settings-entry")),
        )
        assertSerializationRoundTrip(
            AppSettingsRoute(sessionId, WireNavEntryId("app-settings-entry")),
        )
        assertSerializationRoundTrip(
            NetworkSettingsRoute(sessionId, WireNavEntryId("network-settings-entry")),
        )
        assertSerializationRoundTrip(
            PrivacySettingsRoute(sessionId, WireNavEntryId("privacy-settings-entry")),
        )
        assertSerializationRoundTrip(
            CustomizationSettingsRoute(sessionId, WireNavEntryId("customization-settings-entry")),
        )
        assertSerializationRoundTrip(
            LicensesSettingsRoute(sessionId, WireNavEntryId("licenses-settings-entry")),
        )
        assertSerializationRoundTrip(
            DependenciesSettingsRoute(sessionId, WireNavEntryId("dependencies-settings-entry")),
        )
        assertSerializationRoundTrip(
            BackupAndRestoreSettingsRoute(sessionId, WireNavEntryId("backup-settings-entry")),
        )
        assertSerializationRoundTrip(
            AboutThisAppRoute(sessionId, WireNavEntryId("about-entry")),
        )
        assertSerializationRoundTrip(
            MyAccountRoute(sessionId, WireNavEntryId("account-entry")),
        )
        assertSerializationRoundTrip(
            ChangeEmailRoute(sessionId, WireNavEntryId("change-email-entry")),
        )
        assertSerializationRoundTrip(
            VerifyEmailRoute(sessionId, "alice@example.com", WireNavEntryId("verify-email-entry")),
        )
        assertSerializationRoundTrip(
            ChangeUserColorRoute(sessionId, WireNavEntryId("color-entry")),
        )
        assertSerializationRoundTrip(
            ChangeHandleRoute(sessionId, WireNavEntryId("handle-entry")),
        )
        assertSerializationRoundTrip(
            ChangeDisplayNameRoute(sessionId, WireNavEntryId("display-name-entry")),
        )
    }

    @Test
    fun givenSameSettingsDestination_whenCreatingTwoEntries_thenEntryIdentityIsUniqueAndRouteIdIsStable() {
        val first = PrivacySettingsRoute(sessionId)
        val second = PrivacySettingsRoute(sessionId)

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(PrivacySettingsRoute.ROUTE_ID, first.routeId)
        assertEquals(first.routeId, second.routeId)
    }

    @Test
    fun givenSettingsRoutes_whenReadingRouteIds_thenLegacyAnalyticsBaseRoutesArePreserved() {
        val expectations = mapOf(
            SettingsRoute(sessionId) to "app/settings_screen",
            AppSettingsRoute(sessionId) to "app/app_settings_screen",
            NetworkSettingsRoute(sessionId) to "app/network_settings_screen",
            PrivacySettingsRoute(sessionId) to "app/privacy_settings_config_screen",
            CustomizationSettingsRoute(sessionId) to "app/customization_screen",
            LicensesSettingsRoute(sessionId) to "app/licenses_screen",
            DependenciesSettingsRoute(sessionId) to "app/dependencies_screen",
            BackupAndRestoreSettingsRoute(sessionId) to "app/backup_and_restore_screen",
            AboutThisAppRoute(sessionId) to "app/about_this_app_screen",
            MyAccountRoute(sessionId) to "app/my_account_screen",
            ChangeEmailRoute(sessionId) to "app/change_email_screen",
            VerifyEmailRoute(sessionId, "alice@example.com") to "app/verify_email_screen",
            ChangeUserColorRoute(sessionId) to "app/change_user_color_screen",
            ChangeHandleRoute(sessionId) to "app/change_handle_screen",
            ChangeDisplayNameRoute(sessionId) to "app/change_display_name_screen",
        )

        expectations.forEach { (route, expectedRouteId) ->
            assertEquals(expectedRouteId, route.routeId)
        }
    }

    @Test
    fun givenMigratedSettingsItems_whenMappingToNavigation3_thenExpectedTypedRoutesAreReturned() {
        val entryId = WireNavEntryId("mapped-settings-entry")
        val expectations = listOf(
            SettingsItem.AppSettings to AppSettingsRoute(sessionId, entryId),
            SettingsItem.YourAccount to MyAccountRoute(sessionId, entryId),
            SettingsItem.AboutApp to AboutThisAppRoute(sessionId, entryId),
            SettingsItem.NetworkSettings to NetworkSettingsRoute(sessionId, entryId),
            SettingsItem.PrivacySettings to PrivacySettingsRoute(sessionId, entryId),
            SettingsItem.Customization to CustomizationSettingsRoute(sessionId, entryId),
            SettingsItem.Licenses to LicensesSettingsRoute(sessionId, entryId),
            SettingsItem.Dependencies to DependenciesSettingsRoute(sessionId, entryId),
            SettingsItem.BackupAndRestore to BackupAndRestoreSettingsRoute(sessionId, entryId),
        )

        expectations.forEach { (item, route) ->
            assertEquals(route, item.toNavigation3Route(sessionId, entryId))
        }
    }

    @Test
    fun givenSettingsItemOutsideMigratedBatches_whenMappingToNavigation3_thenNoRouteIsClaimed() {
        assertNull(SettingsItem.ManageDevices.toNavigation3Route(sessionId))
    }

    @Test
    fun givenEverySettingsDirection_whenResolvingNavigation3Target_thenTypedOrExternalTargetIsReturned() {
        val typedItems = listOf(
            SettingsItem.AppSettings,
            SettingsItem.YourAccount,
            SettingsItem.AboutApp,
            SettingsItem.NetworkSettings,
            SettingsItem.PrivacySettings,
            SettingsItem.Customization,
            SettingsItem.Licenses,
            SettingsItem.Dependencies,
            SettingsItem.BackupAndRestore,
        )
        typedItems.forEach { item ->
            val target = item.toNavigation3Target(sessionId)

            assertEquals(
                item.toNavigation3Route(sessionId)?.routeId,
                (target as SettingsNavigation3Target.Route).route.routeId,
            )
        }

        val externalItems = mapOf(
            SettingsItem.ManageDevices to SettingsNavigation3Destination.MANAGE_DEVICES,
            SettingsItem.TermsOfUse to SettingsNavigation3Destination.TERMS_OF_USE,
            SettingsItem.WireWebsite to SettingsNavigation3Destination.WIRE_WEBSITE,
            SettingsItem.PrivacyPolicy to SettingsNavigation3Destination.PRIVACY_POLICY,
            SettingsItem.Support to SettingsNavigation3Destination.SUPPORT,
            SettingsItem.ReportMisuse to SettingsNavigation3Destination.REPORT_MISUSE,
            SettingsItem.DebugSettings to SettingsNavigation3Destination.DEBUG_SETTINGS,
            SettingsItem.GiveFeedback to SettingsNavigation3Destination.GIVE_FEEDBACK,
        )
        externalItems.forEach { (item, destination) ->
            assertEquals(
                SettingsNavigation3Target.External(destination),
                item.toNavigation3Target(sessionId),
            )
        }
    }

    @Test
    fun givenBlankEmail_whenCreatingVerifyEmailRoute_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifyEmailRoute(sessionId, "")
        }
    }

    @Test
    fun givenAccountUpdateResult_whenSerializedAndRestored_thenSuccessValueIsPreserved() {
        val result = SettingsAccountUpdateResult(successful = false)

        assertEquals(
            result,
            Json.decodeFromString<SettingsAccountUpdateResult>(Json.encodeToString(result)),
        )
        assertEquals("settings.account-update", SettingsAccountUpdateResultContract.id.value)
    }

    private inline fun <reified T : SessionRoute> assertSerializationRoundTrip(
        route: T,
    ) {
        val restored = Json.decodeFromString<T>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals(sessionId, restored.sessionId)
        assertEquals(route.entryId, restored.entryId)
        assertEquals(route.routeId, restored.routeId)
    }
}
