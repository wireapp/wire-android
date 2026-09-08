/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.AuthenticationLoginArguments
import com.wire.android.navigation.routes.auth.AuthenticationLoginPasswordPath
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.AuthenticationNavigationTransition
import com.wire.android.navigation.routes.auth.AuthenticationPrefilledUserIdentifier
import com.wire.android.navigation.routes.auth.AuthenticationSsoCodeAutoLogin
import com.wire.android.navigation.routes.auth.LoginRoute
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.android.navigation.routes.auth.toAuthenticationSsoLoginResult
import com.wire.android.navigation.routes.media.AuthenticatedImportMediaRoute
import com.wire.android.navigation.routes.media.LoggedOutImportMediaRoute
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.home.conversations.ConversationRouteId
import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import kotlinx.coroutines.flow.Flow

@Composable
internal fun HandleNavigation3ViewActions(
    actions: Flow<WireActivityViewAction>,
    runtime: WireNavigation3Runtime,
    authenticationRouter: AuthenticationNavigation3Router,
    loginTypeSelector: LoginTypeSelector,
    currentSessionId: () -> WireSessionId?,
) {
    val context = LocalContext.current
    HandleActions(actions) { action ->
        val resolution = WireActivityNavigation3EffectResolver.resolve(
            action = action,
            routes = runtime.navigator.routes,
            currentSessionId = currentSessionId(),
            canUseNewLogin = loginTypeSelector.canUseNewLogin(),
        )
        resolution.mutations.forEach { mutation ->
            when (mutation) {
                is WireActivityNavigation3Mutation.Navigate ->
                    authenticationRouter.navigate(
                        AuthenticationNavigationTransition.ACTIVITY_EFFECT,
                        mutation.command,
                    )

                is WireActivityNavigation3Mutation.ReplaceCurrent ->
                    authenticationRouter.replaceCurrent(
                        AuthenticationNavigationTransition.ACTIVITY_EFFECT,
                        mutation.route,
                    )
            }
        }
        resolution.toastMessageResId?.let { showNavigation3Toast(context, it) }
    }
}

internal data class WireActivityNavigation3EffectResolution(
    val mutations: List<WireActivityNavigation3Mutation> = emptyList(),
    val toastMessageResId: Int? = null,
)

internal sealed interface WireActivityNavigation3Mutation {
    data class Navigate(val command: WireNavigationCommand) : WireActivityNavigation3Mutation
    data class ReplaceCurrent(val route: WireRoute) : WireActivityNavigation3Mutation
}

@Suppress("TooManyFunctions")
internal object WireActivityNavigation3EffectResolver {

    fun resolve(
        action: WireActivityViewAction,
        routes: List<WireRoute>,
        currentSessionId: WireSessionId?,
        canUseNewLogin: Boolean,
    ): WireActivityNavigation3EffectResolution = when (action) {
        OnAuthorizationNeeded -> authorizationNeeded(routes)
        is OnMigrationLogin -> migration(action, routes, canUseNewLogin)
        is OnAutomaticLogin -> automaticLogin(action, routes)
        is OnCustomBackendLogin -> customBackendLogin(action, routes)
        is OnOpenUserProfile -> openUserProfile(action, currentSessionId)
        is OnSSOLogin -> ssoLogin(action, routes)
        OnShowImportMediaScreen -> importMedia(currentSessionId)
        is OpenConversation -> openConversation(action, currentSessionId)
        OnUnknownDeepLink -> unknownDeepLink(routes)
        is ShowToast -> WireActivityNavigation3EffectResolution(
            toastMessageResId = action.messageResId
        )
    }

    private fun authorizationNeeded(routes: List<WireRoute>) =
        WireActivityNavigation3EffectResolution(
            mutations = if (routes.startsWithEmptyWelcome()) {
                listOf(navigate(newLoginRoute(), WireBackStackMode.CLEAR_WHOLE))
            } else {
                emptyList()
            },
            toastMessageResId = R.string.deeplink_authorization_needed,
        )

    private fun unknownDeepLink(routes: List<WireRoute>) =
        WireActivityNavigation3EffectResolution(
            mutations = if (routes.startsWithEmptyWelcome()) {
                listOf(navigate(newLoginRoute(), WireBackStackMode.CLEAR_WHOLE))
            } else {
                emptyList()
            }
        )

    private fun migration(
        action: OnMigrationLogin,
        routes: List<WireRoute>,
        canUseNewLogin: Boolean,
    ): WireActivityNavigation3EffectResolution {
        val args = AuthenticationLoginArguments(
            userHandle = AuthenticationPrefilledUserIdentifier(action.result.userHandle)
        )
        return if (canUseNewLogin) {
            newLoginNavigation(args, routes)
        } else {
            singleNavigation(
                LoginRoute(args = args, flowId = routes.legacyAuthenticationFlowId()),
                routes.welcomeOrLoginReplacementMode(),
            )
        }
    }

    private fun automaticLogin(
        action: OnAutomaticLogin,
        routes: List<WireRoute>,
    ): WireActivityNavigation3EffectResolution {
        val args = AuthenticationLoginArguments(
            loginPasswordPath = AuthenticationLoginPasswordPath(
                customServerConfig = action.serverLinks?.toAuthenticationServerLinks()
            ),
            ssoCodeAutoLogin = action.ssoCode?.let {
                AuthenticationSsoCodeAutoLogin(
                    ssoCode = it,
                    autoInitiateLogin = true,
                    nomadServiceUrl = action.nomadServiceUrl,
                    cookieLabel = action.cookieLabel,
                )
            },
        )
        return if (action.useNewLogin) {
            newLoginNavigation(args, routes)
        } else {
            singleNavigation(
                LoginRoute(args = args, flowId = routes.legacyAuthenticationFlowId()),
                routes.welcomeOrLoginReplacementMode(),
            )
        }
    }

    private fun customBackendLogin(
        action: OnCustomBackendLogin,
        routes: List<WireRoute>,
    ): WireActivityNavigation3EffectResolution {
        val serverLinks = action.serverLinks.toAuthenticationServerLinks()
        val args = AuthenticationLoginArguments(
            loginPasswordPath = AuthenticationLoginPasswordPath(customServerConfig = serverLinks),
            showBackendConfigSuccess = true,
        )
        return if (action.useNewLogin) {
            newLoginNavigation(args, routes)
        } else {
            singleNavigation(
                WelcomeRoute(
                    customServerConfig = serverLinks,
                    flowId = routes.legacyAuthenticationFlowId(),
                ),
                routes.welcomeOrLoginReplacementMode(),
            )
        }
    }

    private fun ssoLogin(
        action: OnSSOLogin,
        routes: List<WireRoute>,
    ): WireActivityNavigation3EffectResolution {
        val typedResult = action.result.toAuthenticationSsoLoginResult()
        val current = routes.lastOrNull()
        return if (current is NewLoginRoute) {
            WireActivityNavigation3EffectResolution(
                mutations = listOf(
                    WireActivityNavigation3Mutation.ReplaceCurrent(
                        current.copy(
                            args = current.args.copy(ssoLoginResult = typedResult),
                            flowId = current.flowId,
                            entryId = current.entryId,
                        )
                    )
                )
            )
        } else {
            singleNavigation(
                LoginRoute(
                    args = AuthenticationLoginArguments(ssoLoginResult = typedResult),
                    flowId = routes.legacyAuthenticationFlowId(),
                ),
                WireBackStackMode.UPDATE_EXISTING,
            )
        }
    }

    private fun openConversation(
        action: OpenConversation,
        currentSessionId: WireSessionId?,
    ): WireActivityNavigation3EffectResolution {
        val sessionId = action.result.targetSessionId?.let {
            WireSessionId(it.value, it.domain)
        } ?: requireSession(currentSessionId, action)
        val mutations = buildList {
            if (action.result.switchedAccount) {
                add(navigate(HomeRoute(sessionId), WireBackStackMode.CLEAR_WHOLE))
            }
            add(
                navigate(
                    ConversationRoute(
                        sessionId = sessionId,
                        conversationId = ConversationRouteId(
                            action.result.conversationId.value,
                            action.result.conversationId.domain,
                        ),
                    ),
                    WireBackStackMode.UPDATE_EXISTING,
                )
            )
        }
        return WireActivityNavigation3EffectResolution(mutations)
    }

    private fun openUserProfile(
        action: OnOpenUserProfile,
        currentSessionId: WireSessionId?,
    ): WireActivityNavigation3EffectResolution {
        val sessionId = action.result.targetSessionId?.let {
            WireSessionId(it.value, it.domain)
        } ?: requireSession(currentSessionId, action)
        val mutations = buildList {
            if (action.result.switchedAccount) {
                add(navigate(HomeRoute(sessionId), WireBackStackMode.CLEAR_WHOLE))
            }
            add(
                navigate(
                    OtherUserProfileRoute(
                        sessionId = sessionId,
                        targetUserId = UserProfileQualifiedId(
                            action.result.userId.value,
                            action.result.userId.domain,
                        ),
                    ),
                    WireBackStackMode.UPDATE_EXISTING,
                )
            )
        }
        return WireActivityNavigation3EffectResolution(mutations)
    }

    private fun importMedia(currentSessionId: WireSessionId?) =
        singleNavigation(
            destination = currentSessionId?.let(::AuthenticatedImportMediaRoute)
                ?: LoggedOutImportMediaRoute(),
            mode = WireBackStackMode.UPDATE_EXISTING,
        )

    private fun singleNavigation(
        destination: WireRoute,
        mode: WireBackStackMode,
    ) = WireActivityNavigation3EffectResolution(listOf(navigate(destination, mode)))

    private fun newLoginNavigation(
        args: AuthenticationLoginArguments,
        routes: List<WireRoute>,
    ): WireActivityNavigation3EffectResolution {
        val currentLoginRoot = routes.singleOrNull() as? NewLoginRoute
        return if (currentLoginRoot != null) {
            WireActivityNavigation3EffectResolution(
                mutations = listOf(
                    WireActivityNavigation3Mutation.ReplaceCurrent(
                        currentLoginRoot.copy(args = args)
                    )
                )
            )
        } else {
            singleNavigation(newLoginRoute(args), routes.welcomeOrLoginReplacementMode())
        }
    }

    private fun navigate(
        destination: WireRoute,
        mode: WireBackStackMode,
    ) = WireActivityNavigation3Mutation.Navigate(
        WireNavigationCommand(destination = destination, backStackMode = mode)
    )

    private fun requireSession(
        currentSessionId: WireSessionId?,
        action: WireActivityViewAction,
    ): WireSessionId = checkNotNull(currentSessionId) {
        "${action::class.simpleName} requires an active session"
    }

    private fun newLoginRoute(
        args: AuthenticationLoginArguments = AuthenticationLoginArguments(),
    ): NewLoginRoute = NewLoginRoute.start(args)

    private fun List<WireRoute>.startsWithEmptyWelcome(): Boolean =
        firstOrNull() is NewWelcomeEmptyStartRoute

    private fun List<WireRoute>.welcomeOrLoginReplacementMode(): WireBackStackMode =
        if (firstOrNull().isAuthenticationStartRoute()) {
            WireBackStackMode.CLEAR_WHOLE
        } else {
            WireBackStackMode.UPDATE_EXISTING
        }

    private fun List<WireRoute>.legacyAuthenticationFlowId(): String = when (val route = lastOrNull()) {
        is WelcomeRoute -> route.flowId
        is LoginRoute -> route.flowId
        else -> WireNavEntryId.random().value
    }

    private fun WireRoute?.isAuthenticationStartRoute(): Boolean =
        this is NewWelcomeEmptyStartRoute ||
            this is WelcomeRoute ||
            this is NewLoginRoute ||
            this is LoginRoute
}

private fun showNavigation3Toast(context: Context, messageResId: Int) {
    Toast.makeText(
        context,
        context.resources.getString(messageResId),
        Toast.LENGTH_SHORT,
    ).show()
}
