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

@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.newConversationViewModel
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessOnCreateRouteScreen
import com.wire.android.ui.home.newconversation.channelhistory.ChannelHistoryRoute
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionRouteScreen
import com.wire.android.ui.home.newconversation.groupname.NewGroupNameRouteScreen
import com.wire.android.ui.home.newconversation.groupsearch.NewGroupConversationSearchPeopleRouteScreen
import com.wire.android.ui.home.newconversation.search.NewConversationSearchPeopleRouteScreen
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner

/**
 * Cross-feature exits from the new-conversation flow.
 *
 * These use KMP-safe primitive identities. Each target feature will replace the callback with its
 * own typed route as that feature is migrated, without changing the screen adapters below.
 */
internal interface NewConversationNavigation3Actions {
    fun exitFlow()
    fun openUserProfile(userIdValue: String, userIdDomain: String)
    fun openServiceDetails(
        serviceId: String,
        providerId: String,
        useNewAppsUi: Boolean,
    )

    /**
     * Replaces the complete new-conversation flow with the created conversation, matching legacy
     * `REMOVE_CURRENT_NESTED_GRAPH`.
     */
    fun completeFlowWithCreatedConversation(
        conversationIdValue: String,
        conversationIdDomain: String,
    )

    /**
     * Clears the whole stack and opens Home, matching the legacy discard action.
     */
    fun discardFlowToHome()
    fun openTeamPlan()
}

/**
 * Navigation 3 entries for all new-conversation steps except the separately migrated
 * channel-history pilot.
 *
 * The start route retains the legacy bottom-up presentation while subsequent steps retain the
 * nested graph's standard horizontal transition.
 */
internal fun newConversationNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: NewConversationNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<NewConversationSearchPeopleRoute>(
        presentation = WireEntryPresentation.PopUp,
    ) { route ->
        NewConversationSearchPeopleNavigation3Entry(route, runtime, actions)
    }
    wireEntry<NewGroupConversationSearchPeopleRoute> { route ->
        NewGroupConversationSearchPeopleNavigation3Entry(route, runtime, actions)
    }
    wireEntry<NewGroupNameRoute> { route ->
        NewGroupNameNavigation3Entry(route, runtime, actions)
    }
    wireEntry<GroupOptionRoute> { route ->
        GroupOptionNavigation3Entry(route, runtime, actions)
    }
    wireEntry<ChannelAccessOnCreateRoute> { route ->
        ChannelAccessOnCreateNavigation3Entry(route, runtime)
    }
}

@Composable
private fun NewConversationSearchPeopleNavigation3Entry(
    route: NewConversationSearchPeopleRoute,
    runtime: WireNavigation3Runtime,
    actions: NewConversationNavigation3Actions,
) {
    val viewModel = newConversationFlowViewModel(route.flowId)
    NewConversationSearchPeopleRouteScreen(
        newConversationViewModel = viewModel,
        onNavigateBack = {
            if (!runtime.navigator.goBack()) actions.exitFlow()
        },
        onOpenUserProfile = actions::openUserProfile,
        onOpenServiceDetails = actions::openServiceDetails,
        onStartGroupOrChannel = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    NewGroupConversationSearchPeopleRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    )
                )
            )
        },
        onOpenTeamPlan = actions::openTeamPlan,
    )
}

@Composable
private fun NewGroupConversationSearchPeopleNavigation3Entry(
    route: NewGroupConversationSearchPeopleRoute,
    runtime: WireNavigation3Runtime,
    actions: NewConversationNavigation3Actions,
) {
    NewGroupConversationSearchPeopleRouteScreen(
        newConversationViewModel = newConversationFlowViewModel(route.flowId),
        onNavigateBack = { runtime.navigator.goBack() },
        onOpenUserProfile = actions::openUserProfile,
        onContinue = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    NewGroupNameRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    )
                )
            )
        },
    )
}

@Composable
private fun NewGroupNameNavigation3Entry(
    route: NewGroupNameRoute,
    runtime: WireNavigation3Runtime,
    actions: NewConversationNavigation3Actions,
) {
    NewGroupNameRouteScreen(
        newConversationViewModel = newConversationFlowViewModel(route.flowId),
        onConversationCreated = {
            actions.completeFlowWithCreatedConversation(it.value, it.domain)
        },
        onContinueToOptions = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    GroupOptionRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    )
                )
            )
        },
        onEditParticipants = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = NewGroupConversationSearchPeopleRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    ),
                    backStackMode = WireBackStackMode.UPDATE_EXISTING,
                )
            )
        },
        onDiscard = actions::discardFlowToHome,
        onNavigateBack = { runtime.navigator.goBack() },
    )
}

@Composable
private fun GroupOptionNavigation3Entry(
    route: GroupOptionRoute,
    runtime: WireNavigation3Runtime,
    actions: NewConversationNavigation3Actions,
) {
    GroupOptionRouteScreen(
        newConversationViewModel = newConversationFlowViewModel(route.flowId),
        onConversationCreated = {
            actions.completeFlowWithCreatedConversation(it.value, it.domain)
        },
        onOpenAccess = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    ChannelAccessOnCreateRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    )
                )
            )
        },
        onOpenHistory = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    ChannelHistoryRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    )
                )
            )
        },
        onEditParticipants = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = NewGroupConversationSearchPeopleRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                    ),
                    backStackMode = WireBackStackMode.UPDATE_EXISTING,
                )
            )
        },
        onDiscard = actions::discardFlowToHome,
        onNavigateBack = { runtime.navigator.goBack() },
    )
}

@Composable
private fun ChannelAccessOnCreateNavigation3Entry(
    route: ChannelAccessOnCreateRoute,
    runtime: WireNavigation3Runtime,
) {
    ChannelAccessOnCreateRouteScreen(
        newConversationViewModel = newConversationFlowViewModel(route.flowId),
        onNavigateBack = { runtime.navigator.goBack() },
    )
}

@Composable
private fun newConversationFlowViewModel(flowId: String): NewConversationViewModel {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(flowId))
    return newConversationViewModel(viewModelStoreOwner = flowOwner)
}
