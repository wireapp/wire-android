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

import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.feature.meetings.ui.create.MeetingsNavigation3Actions
import com.wire.android.feature.meetings.ui.create.MeetingsNavigation3Contribution
import com.wire.android.feature.sketch.navigation.SketchNavigation3Contribution
import com.wire.android.feature.cells.navigation.CellsNavigation3Contribution
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Actions
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Contribution
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.media.MediaNavigation3Actions
import com.wire.android.navigation.routes.media.MediaNavigation3Contribution
import com.wire.android.navigation.routes.utility.InitialSyncNavigation3Actions
import com.wire.android.navigation.routes.utility.UtilityNavigation3Contribution
import com.wire.android.ui.home.HomeNavigation3Actions
import com.wire.android.ui.home.HomeNavigation3Contribution
import com.wire.android.ui.home.HomeTopLevelNavigation3Actions
import com.wire.android.ui.home.appLock.AppLockNavigation3Actions
import com.wire.android.ui.home.appLock.AppLockNavigation3Contribution
import com.wire.android.ui.home.conversations.ConversationAuxNavigation3Actions
import com.wire.android.ui.home.conversations.ConversationAuxNavigation3Contribution
import com.wire.android.ui.home.conversations.ConversationNavigation3Actions
import com.wire.android.ui.home.conversations.ConversationNavigation3Contribution
import com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Actions
import com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Contribution
import com.wire.android.ui.home.newconversation.NewConversationNavigation3Actions
import com.wire.android.ui.home.newconversation.newConversationNavigation3Entries
import com.wire.android.ui.home.newconversation.channelhistory.ChannelHistoryNavigation3Pilot
import com.wire.android.ui.home.settings.SettingsNavigation3Actions
import com.wire.android.ui.home.settings.SettingsNavigation3Contribution
import com.wire.android.ui.settings.devices.DeviceE2EINavigation3Actions
import com.wire.android.ui.settings.devices.DeviceE2EINavigation3Contribution
import com.wire.android.ui.userprofile.UserProfileNavigation3Actions
import com.wire.android.ui.userprofile.UserProfileNavigation3Contribution
import com.wire.android.ui.userprofile.teammigration.TeamMigrationNavigation3Actions
import com.wire.android.ui.userprofile.teammigration.TeamMigrationNavigation3Contribution

/**
 * The one host boundary required by every currently migrated Navigation 3 contribution.
 *
 * [HomeNavigation3Actions] deliberately exposes its existing nested callback bundles for the
 * Conversations and Cells top-level content. Its top-level and settings interfaces, however, are
 * this same host contract, avoiding a second action implementation with subtly different state.
 */
internal interface WireNavigation3CompositeActions :
    AuthenticationNavigation3Actions,
    HomeNavigation3Actions,
    HomeTopLevelNavigation3Actions,
    NewConversationNavigation3Actions,
    SettingsNavigation3Actions,
    DeviceE2EINavigation3Actions,
    UserProfileNavigation3Actions,
    TeamMigrationNavigation3Actions,
    ConversationNavigation3Actions,
    ConversationAuxNavigation3Actions,
    ConversationDetailsNavigation3Actions,
    MediaNavigation3Actions,
    InitialSyncNavigation3Actions,
    AppLockNavigation3Actions,
    MeetingsNavigation3Actions {

    override val topLevel: HomeTopLevelNavigation3Actions
        get() = this

    override val settings: SettingsNavigation3Actions
        get() = this

    fun exitCellsFlow()
}

/**
 * Complete production-host input assembled from the independently owned feature contributions.
 */
internal data class WireNavigation3ContributionCatalog(
    val resultTypes: List<WireNavigation3ResultType<*>>,
    val entryProviderInstallers: List<WireEntryProviderInstaller>,
)

/**
 * App-owned composition root for the migrated Navigation 3 surface.
 *
 * Ordering is intentional and stable: roots precede their feature details, then utility and
 * app-lock overlays finish the registry. An entry type must be owned by exactly one contribution.
 */
internal object WireNavigation3Contributions {
    const val EXPECTED_ROUTE_REGISTRATION_COUNT: Int = 107
    const val EXPECTED_INSTALLER_COUNT: Int = 19

    fun create(
        runtime: WireNavigation3Runtime,
        actions: WireNavigation3CompositeActions,
        authenticationRouter: AuthenticationNavigation3Router,
    ): WireNavigation3ContributionCatalog {
        return WireNavigation3ContributionCatalog(
            resultTypes = resultTypes(),
            entryProviderInstallers = entryProviderInstallers(runtime, actions, authenticationRouter),
        )
    }

    fun resultTypes(): List<WireNavigation3ResultType<*>> =
        buildList {
            addAll(AuthenticationNavigation3Contribution.resultTypes)
            addAll(ChannelHistoryNavigation3Pilot.resultTypes)
            addAll(SettingsNavigation3Contribution.resultTypes)
            addAll(DeviceE2EINavigation3Contribution.resultTypes)
            addAll(UserProfileNavigation3Contribution.resultTypes)
            addAll(ConversationNavigation3Contribution.resultTypes)
            addAll(ConversationAuxNavigation3Contribution.resultTypes)
            addAll(ConversationDetailsNavigation3Contribution.resultTypes)
            addAll(MediaNavigation3Contribution.resultTypes)
            addAll(UtilityNavigation3Contribution.resultTypes)
            addAll(AppLockNavigation3Contribution.resultTypes)
            addAll(CellsNavigation3Contribution.resultTypes)
            addAll(SketchNavigation3Contribution.resultTypes)
        }.requireUniqueResultContractIds()

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: WireNavigation3CompositeActions,
        authenticationRouter: AuthenticationNavigation3Router = AuthenticationNavigation3Router(runtime),
    ): List<WireEntryProviderInstaller> =
        buildList {
            // Authentication includes the create-account installer.
            addAll(AuthenticationNavigation3Contribution.entryProviderInstallers(actions, authenticationRouter))
            addAll(HomeNavigation3Contribution.entryProviderInstallers(runtime, actions))
            add(newConversationNavigation3Entries(runtime, actions))
            addAll(ChannelHistoryNavigation3Pilot.entryProviderInstallers(runtime))
            addAll(SettingsNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(DeviceE2EINavigation3Contribution.entryProviderInstallers(runtime, actions, authenticationRouter))
            addAll(UserProfileNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(TeamMigrationNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(ConversationNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(ConversationAuxNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(ConversationDetailsNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(MediaNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(UtilityNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(AppLockNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(CellsNavigation3Contribution.entryProviderInstallers(runtime, actions::exitCellsFlow))
            addAll(MeetingsNavigation3Contribution.entryProviderInstallers(runtime, actions))
            addAll(SketchNavigation3Contribution.entryProviderInstallers(runtime))
        }.also { installers ->
            check(installers.size == EXPECTED_INSTALLER_COUNT) {
                "Expected $EXPECTED_INSTALLER_COUNT Navigation 3 installers, found ${installers.size}"
            }
        }
}

private fun List<WireNavigation3ResultType<*>>.requireUniqueResultContractIds(): List<WireNavigation3ResultType<*>> {
    val duplicateIds = groupBy { it.contract.id.value }
        .filterValues { it.size > 1 }
        .keys
    require(duplicateIds.isEmpty()) {
        "Duplicate Navigation 3 result contract ids: ${duplicateIds.sorted()}"
    }
    return this
}
