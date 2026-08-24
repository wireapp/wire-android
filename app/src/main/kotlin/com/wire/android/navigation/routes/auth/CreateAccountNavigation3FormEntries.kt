package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import com.wire.android.ui.authentication.create.code.CreateAccountCodeRouteScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsRouteScreen
import com.wire.android.ui.authentication.createAccountCodeViewModel
import com.wire.android.ui.authentication.createAccountDetailsViewModel
import com.wire.navigation.WireBackStackMode

@Composable
internal fun createAccountDetailsEntry(
    route: CreateAccountDetailsRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountDetailsRouteScreen(
        route = route,
        viewModel = createAccountDetailsViewModel(route.type, route.customServerConfig, entryOwner(route.entryId)),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onCodeRequested = { router.navigate(AuthenticationNavigationTransition.ACCOUNT_DETAILS_TO_CODE, it) },
    )
}

@Composable
internal fun createAccountCodeEntry(
    route: CreateAccountCodeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountCodeRouteScreen(
        viewModel = createAccountCodeViewModel(
            route.type,
            route.registrationInfo,
            route.customServerConfig,
            entryOwner(route.entryId),
        ),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onSuccess = { type, userId ->
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_CODE_TO_SUMMARY,
                CreateAccountSummaryRoute(type, userId.toCreateAccountSessionId(), route.flowId),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
        onTooManyDevices = {
            router.completeLogin(
                route.loginTerminalEventId(),
                AuthenticationLoginCompletion.RemoveDevice(it.toCreateAccountSessionId()),
            )
        },
    )
}
