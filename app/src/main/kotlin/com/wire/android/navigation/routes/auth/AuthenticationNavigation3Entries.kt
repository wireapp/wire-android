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
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginRouteScreen
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.loginEmailViewModel
import com.wire.android.ui.authentication.newLoginViewModel
import com.wire.android.ui.authentication.welcome.WelcomeRouteScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreenAction
import com.wire.android.ui.authentication.welcomeViewModel
import com.wire.android.ui.newauthentication.login.NewLoginAction
import com.wire.android.ui.newauthentication.login.NewLoginRouteScreen
import com.wire.android.ui.newauthentication.login.code.NewLoginVerificationCodeRouteScreen
import com.wire.android.ui.newauthentication.login.password.LoginStateNavigationAndDialogs
import com.wire.android.ui.newauthentication.login.password.NewLoginPasswordRouteScreen
import com.wire.android.ui.newauthentication.login.password.NewLoginPasswordScreenAction
import com.wire.android.ui.newauthentication.welcome.NewWelcomeEmptyStartScreen
import com.wire.android.ui.newauthentication.welcome.WelcomeChooserScreen
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner

/**
 * Cross-batch operations emitted by the first authentication entries.
 *
 * The host owns routes that have not been migrated in this batch. Keeping those boundaries
 * semantic prevents the entries from importing generated destinations or a Navigation 2
 * controller.
 */
internal interface AuthenticationNavigation3Actions {
    fun canUseNewLogin(): Boolean
    fun exitAuthentication()
    fun openUrl(url: String)
    fun openTeamAccountCreation(request: AuthenticationTeamAccountCreationRequest)
}

internal sealed interface AuthenticationLoginCompletion {
    data class Home(val sessionId: WireSessionId) : AuthenticationLoginCompletion
    data class InitialSync(val sessionId: WireSessionId) : AuthenticationLoginCompletion
    data class E2EIEnrollment(val sessionId: WireSessionId) : AuthenticationLoginCompletion
    data class RemoveDevice(val sessionId: WireSessionId) : AuthenticationLoginCompletion
}

internal data class AuthenticationTeamAccountCreationRequest(
    val url: String,
    val returnRoute: NewLoginPasswordRoute,
)

internal object AuthenticationNavigation3Contribution {
    val resultTypes = emptyList<com.wire.android.navigation.navigation3.WireNavigation3ResultType<*>>()

    fun entryProviderInstallers(
        actions: AuthenticationNavigation3Actions,
        router: AuthenticationNavigation3Router,
    ): List<WireEntryProviderInstaller> = listOf(
        authenticationNavigation3Entries(actions, router),
        createAccountNavigation3Entries(actions, router),
    )
}

internal fun authenticationNavigation3Entries(
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
): WireEntryProviderInstaller = {
    wireEntry<WelcomeChooserRoute>(presentation = WireEntryPresentation.None) { route ->
        WelcomeChooserScreen {
            val destination = if (actions.canUseNewLogin()) {
                NewLoginRoute(
                    flowId = route.newLoginFlowId(),
                )
            } else {
                WelcomeRoute(flowId = route.newLoginFlowId())
            }
            router.navigate(AuthenticationNavigationTransition.CHOOSER_TO_LOGIN, destination)
        }
    }
    wireEntry<NewWelcomeEmptyStartRoute>(presentation = WireEntryPresentation.None) {
        NewWelcomeEmptyStartScreen()
    }
    wireEntry<WelcomeRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        WelcomeNavigation3Entry(route, actions, router)
    }
    wireEntry<LoginRoute>(presentation = WireEntryPresentation.Slide) { route ->
        LoginNavigation3Entry(route, actions, router)
    }
    wireEntry<NewLoginRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        NewLoginNavigation3Entry(route, actions, router)
    }
    wireEntry<NewLoginPasswordRoute>(presentation = WireEntryPresentation.Slide) { route ->
        NewLoginPasswordNavigation3Entry(route, actions, router)
    }
    wireEntry<NewLoginVerificationCodeRoute>(presentation = WireEntryPresentation.Slide) { route ->
        NewLoginVerificationCodeNavigation3Entry(route, actions, router)
    }
}

@Composable
private fun WelcomeNavigation3Entry(
    route: WelcomeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    WelcomeRouteScreen(
        viewModel = welcomeViewModel(route.toLegacyNavArgs(), flowOwner),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onAction = { action ->
            when (action) {
                is WelcomeScreenAction.Login -> router.navigate(
                    AuthenticationNavigationTransition.WELCOME_TO_LOGIN,
                    LoginNavArgs(
                            loginPasswordPath = LoginPasswordPath(action.serverConfig)
                        ).toLoginRoute(flowId = route.flowId)
                )

                is WelcomeScreenAction.OpenUrl -> actions.openUrl(action.url)
                is WelcomeScreenAction.CreateTeam -> router.navigate(
                    AuthenticationNavigationTransition.WELCOME_TO_TEAM_ACCOUNT,
                    CreateTeamAccountOverviewRoute(
                            customServerConfig = action.serverConfig.toAuthenticationServerLinks(),
                            flowId = route.createAccountFlowId(),
                        )
                )

                is WelcomeScreenAction.CreatePersonal -> router.navigate(
                    AuthenticationNavigationTransition.WELCOME_TO_PERSONAL_ACCOUNT,
                    CreatePersonalAccountOverviewRoute(
                            customServerConfig = action.serverConfig.toAuthenticationServerLinks(),
                            flowId = route.createAccountFlowId(),
                        )
                )

                is WelcomeScreenAction.CreateAccountData -> router.navigate(
                    AuthenticationNavigationTransition.WELCOME_TO_ACCOUNT_DATA,
                    CreateAccountDataDetailRoute(
                            customServerConfig = action.serverConfig.toAuthenticationServerLinks(),
                            flowId = route.createAccountFlowId(),
                        )
                )
            }
        },
    )
}

@Composable
private fun LoginNavigation3Entry(
    route: LoginRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val legacyArgs = route.toLegacyNavArgs()
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    LoginRouteScreen(
        loginNavArgs = legacyArgs,
        loginEmailViewModel = loginEmailViewModel(legacyArgs, flowOwner),
        onBackPressed = { router.backOrElse(actions::exitAuthentication) },
        onSuccess = { initialSyncCompleted, isE2EIRequired, userId ->
            router.completeLogin(
                eventId = route.terminalLoginEventId(),
                completion = when {
                    isE2EIRequired -> AuthenticationLoginCompletion.E2EIEnrollment(userId.toSessionId())
                    initialSyncCompleted -> AuthenticationLoginCompletion.Home(userId.toSessionId())
                    else -> AuthenticationLoginCompletion.InitialSync(userId.toSessionId())
                },
            )
        },
        onRemoveDeviceNeeded = {
            router.completeLogin(
                route.terminalLoginEventId(),
                AuthenticationLoginCompletion.RemoveDevice(it.toSessionId()),
            )
        },
    )
}

@Composable
private fun NewLoginNavigation3Entry(
    route: NewLoginRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val legacyArgs = route.toLegacyNavArgs()
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    val viewModel = newLoginViewModel(legacyArgs, flowOwner)

    LaunchedEffect(route.args) {
        viewModel.onNavigationArgumentsChanged(legacyArgs)
    }

    LaunchedEffect(route.entryId.value, route.args.ssoLoginResult) {
        legacyArgs.ssoLoginResult?.let(viewModel::handleSSOResult)
    }

    NewLoginRouteScreen(
        navArgs = legacyArgs,
        viewModel = viewModel,
        canNavigateBack = router.canNavigateBack,
        navigateBack = { router.backOrElse(actions::exitAuthentication) },
        onAction = { action ->
            handleNewLoginAction(router, route, viewModel.serverConfig, action, actions)
        },
    )
}

@Composable
private fun NewLoginPasswordNavigation3Entry(
    route: NewLoginPasswordRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val legacyArgs = route.toLegacyNavArgs()
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    val viewModel: LoginEmailViewModel = loginEmailViewModel(
        loginNavArgs = legacyArgs,
        viewModelStoreOwner = flowOwner,
    )

    NewLoginPasswordRouteScreen(
        navArgs = legacyArgs,
        loginEmailViewModel = viewModel,
        canNavigateBack = router.canNavigateBack,
        onAction = { action ->
            when (action) {
                is NewLoginPasswordScreenAction.Success -> router.completeLogin(
                    eventId = route.terminalLoginEventId(),
                    completion = when {
                        action.isE2EIRequired ->
                            AuthenticationLoginCompletion.E2EIEnrollment(action.userId.toSessionId())
                        action.initialSyncCompleted ->
                            AuthenticationLoginCompletion.Home(action.userId.toSessionId())
                        else -> AuthenticationLoginCompletion.InitialSync(action.userId.toSessionId())
                    },
                )

                is NewLoginPasswordScreenAction.RemoveDevice ->
                    router.completeLogin(
                        route.terminalLoginEventId(),
                        AuthenticationLoginCompletion.RemoveDevice(action.userId.toSessionId()),
                    )

                NewLoginPasswordScreenAction.Canceled -> {
                    router.backOrElse(actions::exitAuthentication)
                    true
                }
                is NewLoginPasswordScreenAction.VerificationRequired ->
                    router.navigate(
                        AuthenticationNavigationTransition.PASSWORD_TO_VERIFICATION,
                        action.navArgs.toNewLoginVerificationCodeRoute(route.flowId),
                    )

                is NewLoginPasswordScreenAction.CreateAccountSelector ->
                    router.navigate(
                        AuthenticationNavigationTransition.PASSWORD_TO_ACCOUNT_SELECTOR,
                        CreateAccountSelectorRoute(
                                customServerConfig = action.serverConfig.toAuthenticationServerLinks(),
                                email = action.email,
                                flowId = route.createAccountFlowId(),
                            )
                    )

                is NewLoginPasswordScreenAction.CreatePersonalAccount ->
                    router.navigate(
                        AuthenticationNavigationTransition.PASSWORD_TO_PERSONAL_ACCOUNT,
                        CreatePersonalAccountOverviewRoute(
                                customServerConfig = action.serverConfig.toAuthenticationServerLinks(),
                                flowId = route.createAccountFlowId(),
                            )
                    )
            }
        },
    )
}

@Composable
private fun NewLoginVerificationCodeNavigation3Entry(
    route: NewLoginVerificationCodeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val legacyArgs = route.toLegacyNavArgs()
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    val viewModel: LoginEmailViewModel = loginEmailViewModel(
        loginNavArgs = legacyArgs,
        viewModelStoreOwner = flowOwner,
    )

    LoginStateNavigationAndDialogs(viewModel) { action ->
        when (action) {
            is NewLoginPasswordScreenAction.Success -> router.completeLogin(
                eventId = route.terminalLoginEventId(),
                completion = when {
                    action.isE2EIRequired ->
                        AuthenticationLoginCompletion.E2EIEnrollment(action.userId.toSessionId())
                    action.initialSyncCompleted ->
                        AuthenticationLoginCompletion.Home(action.userId.toSessionId())
                    else -> AuthenticationLoginCompletion.InitialSync(action.userId.toSessionId())
                },
            )

            is NewLoginPasswordScreenAction.RemoveDevice ->
                router.completeLogin(
                    route.terminalLoginEventId(),
                    AuthenticationLoginCompletion.RemoveDevice(action.userId.toSessionId()),
                )

            NewLoginPasswordScreenAction.Canceled -> {
                router.backOrElse(actions::exitAuthentication)
                true
            }
            else -> false
        }
    }
    NewLoginVerificationCodeRouteScreen(
        loginEmailViewModel = viewModel,
        canNavigateBack = router.canNavigateBack,
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
    )
}

private fun handleNewLoginAction(
    router: AuthenticationNavigation3Router,
    route: NewLoginRoute,
    serverConfig: com.wire.kalium.logic.configuration.server.ServerConfig.Links,
    action: NewLoginAction,
    actions: AuthenticationNavigation3Actions,
) {
    when (action) {
        is NewLoginAction.EmailPassword -> router.navigate(
            AuthenticationNavigationTransition.LOGIN_TO_PASSWORD,
            LoginNavArgs(
                    userHandle = com.wire.android.ui.authentication.login.PreFilledUserIdentifierType.PreFilled(
                        action.userIdentifier
                    ),
                    loginPasswordPath = action.loginPasswordPath,
                ).toNewLoginPasswordAttemptRoute(),
        )

        is NewLoginAction.CustomConfig -> router.navigate(
            AuthenticationNavigationTransition.LOGIN_RESTART_CUSTOM_BACKEND,
            LoginNavArgs(
                    userHandle = com.wire.android.ui.authentication.login.PreFilledUserIdentifierType.PreFilled(
                        action.userIdentifier
                    ),
                    loginPasswordPath = LoginPasswordPath(action.customServerConfig),
                ).toNewLoginRoute(route.flowId),
            WireBackStackMode.CLEAR_WHOLE,
        )

        is NewLoginAction.SSO -> actions.openUrl(action.url)
        is NewLoginAction.Success -> router.completeLogin(
            route.terminalLoginEventId(),
            action.nextStep.toAuthenticationLoginCompletion(),
        )
        is NewLoginAction.EnterpriseLoginNotSupported -> {
            router.navigate(
                AuthenticationNavigationTransition.LOGIN_FALLBACK_TO_LEGACY,
                WelcomeRoute(
                    customServerConfig = serverConfig.toAuthenticationServerLinks(),
                    flowId = route.flowId,
                ),
                WireBackStackMode.CLEAR_WHOLE,
            )
            router.navigate(
                AuthenticationNavigationTransition.WELCOME_TO_LOGIN,
                LoginNavArgs(
                        userHandle = com.wire.android.ui.authentication.login.PreFilledUserIdentifierType.PreFilled(
                            userIdentifier = action.userIdentifier,
                            editable = true,
                        ),
                        loginPasswordPath = LoginPasswordPath(serverConfig),
                    ).toLoginRoute(flowId = route.flowId),
            )
        }
    }
}

internal fun NewLoginAction.Success.NextStep.toAuthenticationLoginCompletion(): AuthenticationLoginCompletion =
    when (this) {
        is NewLoginAction.Success.NextStep.None ->
            AuthenticationLoginCompletion.Home(userId.toSessionId())
        is NewLoginAction.Success.NextStep.InitialSync ->
            AuthenticationLoginCompletion.InitialSync(userId.toSessionId())
        is NewLoginAction.Success.NextStep.E2EIEnrollment ->
            AuthenticationLoginCompletion.E2EIEnrollment(userId.toSessionId())
        is NewLoginAction.Success.NextStep.TooManyDevices ->
            AuthenticationLoginCompletion.RemoveDevice(userId.toSessionId())
    }

private fun UserId.toSessionId(): WireSessionId =
    WireSessionId(value = value, domain = domain)

private fun com.wire.navigation.WireRoute.terminalLoginEventId(): String =
    "${entryId.value}:login-terminal"

internal fun WelcomeChooserRoute.newLoginFlowId(): String =
    "new-login:${entryId.value}"

private fun com.wire.navigation.WireRoute.createAccountFlowId(): String =
    "create-account:${entryId.value}"
