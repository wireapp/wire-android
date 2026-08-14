/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.authentication.create.code.CreateAccountCodeRouteScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsRouteScreen
import com.wire.android.ui.authentication.create.email.CreateAccountEmailRouteScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewRouteScreen
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryRouteScreen
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameRouteScreen
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.createAccountCodeViewModel
import com.wire.android.ui.authentication.createAccountDataDetailViewModel
import com.wire.android.ui.authentication.createAccountDetailsViewModel
import com.wire.android.ui.authentication.createAccountEmailViewModel
import com.wire.android.ui.authentication.createAccountOverviewViewModel
import com.wire.android.ui.authentication.createAccountSelectorViewModel
import com.wire.android.ui.authentication.createAccountSummaryViewModel
import com.wire.android.ui.authentication.createAccountUsernameViewModel
import com.wire.android.ui.authentication.createAccountVerificationCodeViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeRouteScreen
import com.wire.android.ui.registration.details.CreateAccountDataDetailRouteScreen
import com.wire.android.ui.registration.selector.CreateAccountSelectorRouteScreen
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireViewModelOwner

internal fun createAccountNavigation3Entries(
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
): WireEntryProviderInstaller = {
    wireEntry<CreateAccountSelectorRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        CreateAccountSelectorNavigation3Entry(route, actions, router)
    }
    wireEntry<CreateAccountDataDetailRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        CreateAccountDataDetailNavigation3Entry(route, actions, router)
    }
    wireEntry<CreateAccountVerificationCodeRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        CreateAccountVerificationNavigation3Entry(route, actions, router)
    }
    wireEntry<CreatePersonalAccountOverviewRoute> { route ->
        CreateAccountOverviewNavigation3Entry(route, CreateAccountFlowType.CreatePersonalAccount, actions, router)
    }
    wireEntry<CreateTeamAccountOverviewRoute> { route ->
        CreateAccountOverviewNavigation3Entry(route, CreateAccountFlowType.CreateTeam, actions, router)
    }
    wireEntry<CreateAccountEmailRoute> { route ->
        CreateAccountEmailNavigation3Entry(route, actions, router)
    }
    wireEntry<CreateAccountDetailsRoute> { route ->
        CreateAccountDetailsNavigation3Entry(route, actions, router)
    }
    wireEntry<CreateAccountCodeRoute> { route ->
        CreateAccountCodeNavigation3Entry(route, actions, router)
    }
    wireEntry<CreateAccountSummaryRoute> { route ->
        CreateAccountSummaryNavigation3Entry(route, router)
    }
    wireEntry<CreateAccountUsernameRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        CreateAccountUsernameNavigation3Entry(route, router)
    }
}

@Composable
private fun CreateAccountSelectorNavigation3Entry(
    route: CreateAccountSelectorRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    val viewModel = createAccountSelectorViewModel(route.toLegacyNavArgs(), owner)
    CreateAccountSelectorRouteScreen(
        viewModel = viewModel,
        onPersonalAccountCreation = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SELECTOR_TO_DATA,
                it.toDataDetailRoute(route.flowId),
            )
        },
        onTeamAccountCreation = { url ->
            val returnRoute = LoginNavArgs(
                userHandle = PreFilledUserIdentifierType.PreFilled(viewModel.email),
                loginPasswordPath = LoginPasswordPath(customServerConfig = viewModel.serverConfig),
            ).toNewLoginPasswordRoute(route.flowId)
            actions.openTeamAccountCreation(
                AuthenticationTeamAccountCreationRequest(url, returnRoute)
            )
        },
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
    )
}

@Composable
private fun CreateAccountDataDetailNavigation3Entry(
    route: CreateAccountDataDetailRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountDataDetailRouteScreen(
        viewModel = createAccountDataDetailViewModel(route.toLegacyNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onCodeRequested = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_DATA_TO_VERIFICATION,
                it.toVerificationCodeRoute(route.flowId),
            )
        },
    )
}

@Composable
private fun CreateAccountVerificationNavigation3Entry(
    route: CreateAccountVerificationCodeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountVerificationCodeRouteScreen(
        viewModel = createAccountVerificationCodeViewModel(route.toLegacyNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onSuccess = { userId ->
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SUMMARY_TO_USERNAME,
                CreateAccountUsernameRoute(userId.toWireSessionId(), route.flowId),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
        onTooManyDevices = {
            router.completeLogin(
                route.terminalLoginEventId(),
                AuthenticationLoginCompletion.RemoveDevice(it.toWireSessionId()),
            )
        },
    )
}

@Composable
private fun CreateAccountOverviewNavigation3Entry(
    route: com.wire.navigation.AuthenticationRoute,
    flowType: CreateAccountFlowType,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val flowId = checkNotNull(route.flowId)
    val owner = createAccountEntryOwner(route.entryId)
    val navArgs = when (route) {
        is CreatePersonalAccountOverviewRoute -> route.toLegacyNavArgs()
        is CreateTeamAccountOverviewRoute -> route.toLegacyNavArgs()
        else -> error("Unsupported create-account overview route ${route::class.qualifiedName}")
    }
    CreateAccountOverviewRouteScreen(
        flowType = flowType,
        viewModel = createAccountOverviewViewModel(navArgs, owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onContinue = {
            router.navigate(
                if (flowType == CreateAccountFlowType.CreatePersonalAccount) {
                    AuthenticationNavigationTransition.PERSONAL_OVERVIEW_TO_EMAIL
                } else {
                    AuthenticationNavigationTransition.TEAM_OVERVIEW_TO_EMAIL
                },
                it.toEmailRoute(flowId),
            )
        },
    )
}

@Composable
private fun CreateAccountEmailNavigation3Entry(
    route: CreateAccountEmailRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountEmailRouteScreen(
        viewModel = createAccountEmailViewModel(route.toLegacyNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onLogin = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_EMAIL_TO_LOGIN,
                LoginRoute(flowId = route.flowId),
                WireBackStackMode.CLEAR_TILL_START,
            )
        },
        onDetailsRequested = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_EMAIL_TO_DETAILS,
                it.toDetailsRoute(route.flowId),
            )
        },
    )
}

@Composable
private fun CreateAccountDetailsNavigation3Entry(
    route: CreateAccountDetailsRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountDetailsRouteScreen(
        viewModel = createAccountDetailsViewModel(route.toLegacyNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onCodeRequested = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_DETAILS_TO_CODE,
                it.toCodeRoute(route.flowId),
            )
        },
    )
}

@Composable
private fun CreateAccountCodeNavigation3Entry(
    route: CreateAccountCodeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountCodeRouteScreen(
        viewModel = createAccountCodeViewModel(route.toLegacyNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onSuccess = { summaryArgs, userId ->
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_CODE_TO_SUMMARY,
                summaryArgs.toSummaryRoute(route.flowId, userId.toWireSessionId()),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
        onTooManyDevices = {
            router.completeLogin(
                route.terminalLoginEventId(),
                AuthenticationLoginCompletion.RemoveDevice(it.toWireSessionId()),
            )
        },
    )
}

@Composable
private fun CreateAccountSummaryNavigation3Entry(
    route: CreateAccountSummaryRoute,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountSummaryRouteScreen(
        viewModel = createAccountSummaryViewModel(route.toLegacyNavArgs(), owner),
        onContinue = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SUMMARY_TO_USERNAME,
                CreateAccountUsernameRoute(route.sessionId, route.flowId),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
    )
}

@Composable
private fun CreateAccountUsernameNavigation3Entry(
    route: CreateAccountUsernameRoute,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    CreateAccountUsernameRouteScreen(
        viewModel = createAccountUsernameViewModel(owner),
        onSuccess = {
            router.completeLogin(
                route.terminalLoginEventId(),
                AuthenticationLoginCompletion.InitialSync(route.sessionId),
            )
        },
    )
}

@Composable
private fun createAccountEntryOwner(entryId: com.wire.navigation.WireNavEntryId): ViewModelStoreOwner =
    wireViewModelStoreOwner(WireViewModelOwner.Entry(entryId))

private fun com.wire.kalium.logic.data.user.UserId.toWireSessionId() =
    com.wire.navigation.WireSessionId(value, domain)

private fun com.wire.navigation.WireRoute.terminalLoginEventId(): String =
    "${entryId.value}:login-terminal"
