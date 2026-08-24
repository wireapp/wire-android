package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import com.wire.android.ui.authentication.create.email.CreateAccountEmailRouteScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewRouteScreen
import com.wire.android.ui.authentication.createAccountEmailViewModel
import com.wire.android.ui.authentication.createAccountOverviewViewModel
import com.wire.navigation.WireBackStackMode

@Composable
internal fun createAccountOverviewEntry(
    route: com.wire.navigation.AuthenticationRoute,
    flowType: CreateAccountRouteFlowType,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val customServerConfig = route.overviewServerConfig()
    CreateAccountOverviewRouteScreen(
        flowType = flowType,
        viewModel = createAccountOverviewViewModel(customServerConfig, entryOwner(route.entryId)),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onContinue = {
            router.navigate(
                flowType.overviewTransition(),
                CreateAccountEmailRoute(flowType, customServerConfig = customServerConfig, flowId = route.flowId),
            )
        },
    )
}

@Composable
internal fun createAccountEmailEntry(
    route: CreateAccountEmailRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountEmailRouteScreen(
        route = route,
        viewModel = createAccountEmailViewModel(route.type, route.customServerConfig, entryOwner(route.entryId)),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onLogin = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_EMAIL_TO_LOGIN,
                LoginRoute(flowId = route.flowId),
                WireBackStackMode.CLEAR_TILL_START,
            )
        },
        onDetailsRequested = { router.navigate(AuthenticationNavigationTransition.ACCOUNT_EMAIL_TO_DETAILS, it) },
    )
}

private fun com.wire.navigation.AuthenticationRoute.overviewServerConfig() = when (this) {
    is CreatePersonalAccountOverviewRoute -> customServerConfig
    is CreateTeamAccountOverviewRoute -> customServerConfig
    else -> error("Unsupported create-account overview route ${this::class.qualifiedName}")
}

private fun CreateAccountRouteFlowType.overviewTransition() = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> AuthenticationNavigationTransition.PERSONAL_OVERVIEW_TO_EMAIL
    CreateAccountRouteFlowType.TEAM -> AuthenticationNavigationTransition.TEAM_OVERVIEW_TO_EMAIL
}
