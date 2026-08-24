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
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.authentication.createAccountDataDetailViewModel
import com.wire.android.ui.authentication.createAccountSelectorViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.registration.details.CreateAccountDataDetailRouteScreen
import com.wire.android.ui.registration.selector.CreateAccountSelectorRouteScreen
import com.wire.android.ui.registration.selector.CreateAccountSelectorNavArgs
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
        createAccountVerificationEntry(route, actions, router)
    }
    wireEntry<CreatePersonalAccountOverviewRoute> { route ->
        createAccountOverviewEntry(route, CreateAccountRouteFlowType.PERSONAL, actions, router)
    }
    wireEntry<CreateTeamAccountOverviewRoute> { route ->
        createAccountOverviewEntry(route, CreateAccountRouteFlowType.TEAM, actions, router)
    }
    wireEntry<CreateAccountEmailRoute> { route ->
        createAccountEmailEntry(route, actions, router)
    }
    wireEntry<CreateAccountDetailsRoute> { route ->
        createAccountDetailsEntry(route, actions, router)
    }
    wireEntry<CreateAccountCodeRoute> { route ->
        createAccountCodeEntry(route, actions, router)
    }
    wireEntry<CreateAccountSummaryRoute> { route ->
        createAccountSummaryCompletionEntry(route, router)
    }
    wireEntry<CreateAccountUsernameRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        createAccountUsernameCompletionEntry(route, router)
    }
}

@Composable
private fun CreateAccountSelectorNavigation3Entry(
    route: CreateAccountSelectorRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val owner = createAccountEntryOwner(route.entryId)
    val viewModel = createAccountSelectorViewModel(
        CreateAccountSelectorNavArgs(route.customServerConfig?.toLegacy(), route.email),
        owner,
    )
    CreateAccountSelectorRouteScreen(
        viewModel = viewModel,
        onPersonalAccountCreation = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SELECTOR_TO_DATA,
                CreateAccountDataDetailRoute(
                    registrationInfo = it.userRegistrationInfo.toRoute(),
                    customServerConfig = it.customServerConfig?.toAuthenticationServerLinks(),
                    flowId = route.flowId,
                ),
            )
        },
        onTeamAccountCreation = { url ->
            val returnRoute = LoginNavArgs(
                userHandle = PreFilledUserIdentifierType.PreFilled(viewModel.email),
                loginPasswordPath = LoginPasswordPath(customServerConfig = viewModel.serverConfig),
            ).toNewLoginPasswordAttemptRoute()
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
        viewModel = createAccountDataDetailViewModel(route.toLegacyDataNavArgs(), owner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onCodeRequested = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_DATA_TO_VERIFICATION,
                CreateAccountVerificationCodeRoute(
                    registrationInfo = it.userRegistrationInfo.toRoute(),
                    customServerConfig = it.customServerConfig?.toAuthenticationServerLinks(),
                    flowId = route.flowId,
                ),
            )
        },
    )
}

@Composable
private fun createAccountEntryOwner(entryId: com.wire.navigation.WireNavEntryId): ViewModelStoreOwner =
    wireViewModelStoreOwner(WireViewModelOwner.Entry(entryId))

private fun CreateAccountDataDetailRoute.toLegacyDataNavArgs(): CreateAccountDataNavArgs =
    CreateAccountDataNavArgs(registrationInfo.toLegacy(), customServerConfig?.toLegacy())

private fun CreateAccountRegistrationInfo.toLegacy(): UserRegistrationInfo =
    UserRegistrationInfo(email, name, firstName, lastName, password, teamName, teamIcon)

private fun UserRegistrationInfo.toRoute(): CreateAccountRegistrationInfo =
    CreateAccountRegistrationInfo(email, name, firstName, lastName, password, teamName, teamIcon)
