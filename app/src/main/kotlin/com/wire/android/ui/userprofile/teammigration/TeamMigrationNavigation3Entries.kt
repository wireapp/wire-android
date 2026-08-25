/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.teammigration

import androidx.compose.runtime.Composable
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.home.settings.teamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.step1.TeamMigrationTeamPlanRouteScreen
import com.wire.android.ui.userprofile.teammigration.step2.TeamMigrationTeamNameRouteScreen
import com.wire.android.ui.userprofile.teammigration.step3.TeamMigrationConfirmationRouteScreen
import com.wire.android.ui.userprofile.teammigration.step4.TeamMigrationDoneRouteScreen
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner

/**
 * Host-owned exits whose destinations live outside the migration flow.
 */
internal interface TeamMigrationNavigation3Actions {
    fun exitMigration()
    fun completeMigrationToHome()
}

internal object TeamMigrationNavigation3Contribution {
    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: TeamMigrationNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(
        teamMigrationNavigation3Entries(runtime, actions)
    )
}

internal fun teamMigrationNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: TeamMigrationNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<TeamMigrationTeamPlanRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        TeamMigrationTeamPlanNavigation3Entry(route, runtime, actions)
    }
    wireEntry<TeamMigrationTeamNameRoute>(presentation = WireEntryPresentation.Slide) { route ->
        TeamMigrationTeamNameNavigation3Entry(route, runtime)
    }
    wireEntry<TeamMigrationConfirmationRoute>(presentation = WireEntryPresentation.Slide) { route ->
        TeamMigrationConfirmationNavigation3Entry(route, runtime)
    }
    wireEntry<TeamMigrationDoneRoute>(presentation = WireEntryPresentation.Slide) { route ->
        TeamMigrationDoneNavigation3Entry(route, actions)
    }
}

@Composable
private fun TeamMigrationTeamPlanNavigation3Entry(
    route: TeamMigrationTeamPlanRoute,
    runtime: WireNavigation3Runtime,
    actions: TeamMigrationNavigation3Actions,
) {
    TeamMigrationTeamPlanRouteScreen(
        teamMigrationViewModel = teamMigrationFlowViewModel(route.flowId),
        isMigrationDotActive = route.isMigrationDotActive,
        onBackButtonClicked = {
            if (!runtime.navigator.goBack()) actions.exitMigration()
        },
        onContinueButtonClicked = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    TeamMigrationTeamNameRoute(route.sessionId, route.flowId)
                )
            )
        },
    )
}

@Composable
private fun TeamMigrationTeamNameNavigation3Entry(
    route: TeamMigrationTeamNameRoute,
    runtime: WireNavigation3Runtime,
) {
    TeamMigrationTeamNameRouteScreen(
        teamMigrationViewModel = teamMigrationFlowViewModel(route.flowId),
        onBackButtonClicked = runtime.navigator::goBack,
        onContinueButtonClicked = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    TeamMigrationConfirmationRoute(route.sessionId, route.flowId)
                )
            )
        },
    )
}

@Composable
private fun TeamMigrationConfirmationNavigation3Entry(
    route: TeamMigrationConfirmationRoute,
    runtime: WireNavigation3Runtime,
) {
    TeamMigrationConfirmationRouteScreen(
        teamMigrationViewModel = teamMigrationFlowViewModel(route.flowId),
        onBackButtonClicked = runtime.navigator::goBack,
        onMigrationCompleted = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = TeamMigrationDoneRoute(route.sessionId, route.flowId),
                    backStackMode = WireBackStackMode.REMOVE_CURRENT_NESTED_FLOW,
                )
            )
        },
        onReturnToFirstStep = { isMigrationDotActive ->
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = TeamMigrationTeamPlanRoute(
                        sessionId = route.sessionId,
                        flowId = route.flowId,
                        isMigrationDotActive = isMigrationDotActive,
                    ),
                    backStackMode = WireBackStackMode.UPDATE_EXISTING,
                )
            )
        },
    )
}

@Composable
private fun TeamMigrationDoneNavigation3Entry(
    route: TeamMigrationDoneRoute,
    actions: TeamMigrationNavigation3Actions,
) {
    TeamMigrationDoneRouteScreen(
        teamMigrationViewModel = teamMigrationFlowViewModel(route.flowId),
        onBackToWireClicked = actions::completeMigrationToHome,
    )
}

@Composable
private fun teamMigrationFlowViewModel(flowId: String): TeamMigrationViewModel {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(flowId))
    return teamMigrationViewModel(viewModelStoreOwner = flowOwner)
}
