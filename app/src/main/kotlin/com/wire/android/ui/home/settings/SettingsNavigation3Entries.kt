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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.home.settings.about.dependencies.DependenciesScreen
import com.wire.android.ui.home.settings.about.licenses.LicensesScreen
import com.wire.android.ui.home.settings.appearance.CustomizationScreen
import com.wire.android.ui.home.settings.appsettings.AppSettingsScreen
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsScreen
import com.wire.android.ui.home.settings.backup.BackupAndRestoreScreen
import com.wire.android.ui.home.settings.privacy.PrivacySettingsConfigScreen
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireSessionId

/**
 * Destinations outside the settings batch.
 *
 * The contribution deliberately exposes semantic actions instead of generated Compose
 * Destinations directions. Each enum value can be replaced by its typed route independently when
 * the owning feature is migrated.
 */
internal enum class SettingsNavigation3Destination {
    ACCOUNT,
    MANAGE_DEVICES,
    TERMS_OF_USE,
    WIRE_WEBSITE,
    PRIVACY_POLICY,
    SUPPORT,
    REPORT_MISUSE,
    DEBUG_SETTINGS,
    GIVE_FEEDBACK,
    ABOUT_APP,
}

internal sealed interface SettingsNavigation3Target {
    data class Route(val route: SessionRoute) : SettingsNavigation3Target
    data class External(val destination: SettingsNavigation3Destination) : SettingsNavigation3Target
}

internal interface SettingsNavigation3Actions {
    fun open(destination: SettingsNavigation3Destination)
    fun openAppLockSetup()
    fun openConversations()

    /**
     * Used only when a detail route is restored/deep-linked without its settings parent.
     */
    fun exitSettings()
}

/**
 * Complete Navigation 3 contribution for the migrated settings batches.
 *
 * The host consumes the installers and result type as one unit, while route and back-stack
 * decisions stay in this feature.
 */
internal object SettingsNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(SettingsAccountUpdateNavigation3ResultType)

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: SettingsNavigation3Actions,
    ): List<WireEntryProviderInstaller> =
        listOf(
            settingsNavigation3Entries(runtime, actions),
            settingsAccountNavigation3Entries(runtime, actions),
        )
}

internal fun settingsNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<SettingsRoute> { route ->
        SettingsNavigation3Root(
            sessionId = route.sessionId,
            runtime = runtime,
            actions = actions,
        )
    }
    wireEntry<AppSettingsRoute> {
        AppSettingsScreen()
    }
    wireEntry<NetworkSettingsRoute> {
        NetworkSettingsScreen(onBackPressed = runtime.backOrExit(actions))
    }
    wireEntry<PrivacySettingsRoute> {
        PrivacySettingsConfigScreen(onBackPressed = runtime.backOrExit(actions))
    }
    wireEntry<CustomizationSettingsRoute> {
        CustomizationScreen(onBackPressed = runtime.backOrExit(actions))
    }
    wireEntry<LicensesSettingsRoute> {
        LicensesScreen(onBackPressed = runtime.backOrExit(actions))
    }
    wireEntry<DependenciesSettingsRoute> {
        DependenciesScreen(onBackPressed = runtime.backOrExit(actions))
    }
    wireEntry<BackupAndRestoreSettingsRoute> {
        BackupAndRestoreScreen(
            onBackPressed = runtime.backOrExit(actions),
            onOpenConversations = actions::openConversations,
        )
    }
}

/**
 * Shared root renderer for a standalone/deep-linked Settings entry and Settings selected inside
 * the Home shell.
 */
@Composable
internal fun SettingsNavigation3Root(
    sessionId: WireSessionId,
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    SettingsScreen(
        lazyListState = lazyListState,
        onOpenAppLockSetup = actions::openAppLockSetup,
        onDirectionItemClicked = { item ->
            when (val target = item.toNavigation3Target(sessionId)) {
                is SettingsNavigation3Target.Route ->
                    runtime.navigator.navigate(WireNavigationCommand(target.route))

                is SettingsNavigation3Target.External ->
                    actions.open(target.destination)
            }
        },
    )
}

private fun WireNavigation3Runtime.backOrExit(
    actions: SettingsNavigation3Actions,
): () -> Unit = {
    if (!navigator.goBack()) actions.exitSettings()
}

internal fun SettingsItem.DirectionItem.toNavigation3Target(
    sessionId: WireSessionId,
): SettingsNavigation3Target =
    toNavigation3Route(sessionId)?.let(SettingsNavigation3Target::Route)
        ?: SettingsNavigation3Target.External(
            when (this) {
                SettingsItem.YourAccount,
                SettingsItem.AboutApp,
                -> error("A typed route was expected for $this")
                SettingsItem.ManageDevices -> SettingsNavigation3Destination.MANAGE_DEVICES
                SettingsItem.TermsOfUse -> SettingsNavigation3Destination.TERMS_OF_USE
                SettingsItem.WireWebsite -> SettingsNavigation3Destination.WIRE_WEBSITE
                SettingsItem.PrivacyPolicy -> SettingsNavigation3Destination.PRIVACY_POLICY
                SettingsItem.Support -> SettingsNavigation3Destination.SUPPORT
                SettingsItem.ReportMisuse -> SettingsNavigation3Destination.REPORT_MISUSE
                SettingsItem.DebugSettings -> SettingsNavigation3Destination.DEBUG_SETTINGS
                SettingsItem.GiveFeedback -> SettingsNavigation3Destination.GIVE_FEEDBACK
                SettingsItem.AppSettings,
                SettingsItem.Customization,
                SettingsItem.NetworkSettings,
                SettingsItem.PrivacySettings,
                SettingsItem.Licenses,
                SettingsItem.Dependencies,
                SettingsItem.BackupAndRestore,
                -> error("A typed route was expected for $this")
            }
        )
