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

import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.utility.InitialSyncRoute
import com.wire.android.navigation.runtime.WireNavigationDiagnostics
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.home.HomeRequirement
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRoute
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.android.ui.settings.devices.SessionSetupDestination
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import com.wire.navigation.authenticationSessionFlowId

/**
 * The executable topology of authentication navigation.
 *
 * Entry providers translate UI callbacks into one of these transitions. Only this router mutates
 * the Navigation 3 stack for authentication screens, making every edge observable and testable.
 */
internal enum class AuthenticationNavigationTransition {
    CHOOSER_TO_LOGIN,
    WELCOME_TO_LOGIN,
    WELCOME_TO_PERSONAL_ACCOUNT,
    WELCOME_TO_TEAM_ACCOUNT,
    WELCOME_TO_ACCOUNT_DATA,
    LOGIN_TO_PASSWORD,
    LOGIN_RESTART_CUSTOM_BACKEND,
    LOGIN_FALLBACK_TO_LEGACY,
    PASSWORD_TO_VERIFICATION,
    PASSWORD_TO_ACCOUNT_SELECTOR,
    PASSWORD_TO_PERSONAL_ACCOUNT,
    ACCOUNT_SELECTOR_TO_DATA,
    ACCOUNT_DATA_TO_VERIFICATION,
    PERSONAL_OVERVIEW_TO_EMAIL,
    TEAM_OVERVIEW_TO_EMAIL,
    ACCOUNT_EMAIL_TO_LOGIN,
    ACCOUNT_EMAIL_TO_DETAILS,
    ACCOUNT_DETAILS_TO_CODE,
    ACCOUNT_CODE_TO_SUMMARY,
    ACCOUNT_SUMMARY_TO_USERNAME,
    AUTH_TO_HOME,
    AUTH_TO_INITIAL_SYNC,
    AUTH_TO_E2EI,
    AUTH_TO_REMOVE_DEVICE,
    AUTH_TO_REGISTER_DEVICE,
    AUTH_TO_USERNAME,
    REGISTER_DEVICE_TO_REMOVE_DEVICE,
    REGISTER_DEVICE_TO_E2EI,
    REMOVE_DEVICE_TO_E2EI,
    E2EI_TO_CERTIFICATE_DETAILS,
    SESSION_SETUP_TO_HOME,
    SESSION_SETUP_TO_INITIAL_SYNC,
    HOME_TO_ADD_ACCOUNT,
    ACCOUNT_SWITCH_TO_HOME,
    ACCOUNT_SWITCH_TO_LOGIN,
    TEAM_WEB_FLOW_TO_PASSWORD,
    SESSION_POLICY,
    ACTIVITY_EFFECT,
    BACK,
}

@Suppress("TooManyFunctions")
internal class AuthenticationNavigation3Router(
    private val runtime: WireNavigation3Runtime,
    private val transitionLedger: AuthenticationTransitionLedger = AuthenticationTransitionLedger(),
) {
    /**
     * Opens the login root requested by an Activity intent.
     *
     * Intents can be delivered again after Activity recreation. Replacing an already active,
     * login root would create a second auth flow (and visibly compose Welcome/Login twice),
     * so that transition is deliberately idempotent here at the mutation boundary.
     */
    fun openLoginFromActivity(): Boolean {
        if (runtime.navigator.routes.hasActiveAuthenticationFlow()) {
            val transitionId = WireNavigationDiagnostics.nextTransitionId()
            WireNavigationDiagnostics.auth(
                transitionId = transitionId,
                event = AuthenticationNavigationTransition.ACTIVITY_EFFECT.name,
                outcome = "duplicate-current-login-ignored",
            )
            return true
        }
        return navigate(
            AuthenticationNavigationTransition.ACTIVITY_EFFECT,
            NewLoginRoute.start(),
            WireBackStackMode.CLEAR_WHOLE,
        )
    }

    fun navigate(
        transition: AuthenticationNavigationTransition,
        destination: WireRoute,
        mode: WireBackStackMode = WireBackStackMode.NONE,
    ): Boolean = navigate(
        transition = transition,
        command = WireNavigationCommand(destination = destination, backStackMode = mode),
    )

    fun navigate(
        transition: AuthenticationNavigationTransition,
        command: WireNavigationCommand,
    ): Boolean {
        val transitionId = WireNavigationDiagnostics.nextTransitionId()
        WireNavigationDiagnostics.auth(
            transitionId = transitionId,
            event = transition.name,
            outcome = "requested:${command.destination.routeId}:${command.backStackMode}",
        )
        return runtime.navigator.navigate(command).also { accepted ->
            WireNavigationDiagnostics.auth(
                transitionId = transitionId,
                event = transition.name,
                outcome = if (accepted) "accepted" else "rejected",
            )
        }
    }

    fun completeLogin(completion: AuthenticationLoginCompletion): Boolean =
        when (completion) {
            is AuthenticationLoginCompletion.Home -> navigate(
                AuthenticationNavigationTransition.AUTH_TO_HOME,
                HomeRoute(completion.sessionId),
                WireBackStackMode.CLEAR_WHOLE,
            )

            is AuthenticationLoginCompletion.InitialSync -> navigate(
                AuthenticationNavigationTransition.AUTH_TO_INITIAL_SYNC,
                InitialSyncRoute(completion.sessionId),
                WireBackStackMode.CLEAR_WHOLE,
            )

            is AuthenticationLoginCompletion.E2EIEnrollment -> navigate(
                AuthenticationNavigationTransition.AUTH_TO_E2EI,
                E2EIEnrollmentRoute(
                    sessionId = completion.sessionId,
                    flowId = currentAuthenticationFlowId(completion.sessionId),
                ),
                WireBackStackMode.CLEAR_WHOLE,
            )

            is AuthenticationLoginCompletion.RemoveDevice -> navigate(
                AuthenticationNavigationTransition.AUTH_TO_REMOVE_DEVICE,
                RemoveDeviceRoute(
                    sessionId = completion.sessionId,
                    flowId = currentAuthenticationFlowId(completion.sessionId),
                ),
                WireBackStackMode.CLEAR_WHOLE,
            )
        }

    /**
     * Applies a terminal login outcome once for the entry that produced [eventId].
     *
     * A rejected navigation is deliberately not consumed and may be retried.
     */
    fun completeLogin(
        eventId: String,
        completion: AuthenticationLoginCompletion,
    ): Boolean =
        when (transitionLedger.executeOnce(eventId) { completeLogin(completion) }) {
            AuthenticationTransitionLedger.Outcome.APPLIED -> true
            AuthenticationTransitionLedger.Outcome.REJECTED -> false
            AuthenticationTransitionLedger.Outcome.ALREADY_APPLIED -> {
                val transitionId = WireNavigationDiagnostics.nextTransitionId()
                WireNavigationDiagnostics.auth(
                    transitionId = transitionId,
                    event = "LOGIN_TERMINAL",
                    outcome = "duplicate-ignored",
                )
                true
            }
        }

    fun completeSessionSetup(
        sessionId: WireSessionId,
        destination: SessionSetupDestination,
    ): Boolean = when (destination) {
        SessionSetupDestination.HOME -> navigate(
            AuthenticationNavigationTransition.SESSION_SETUP_TO_HOME,
            HomeRoute(sessionId),
            WireBackStackMode.CLEAR_WHOLE,
        )

        SessionSetupDestination.INITIAL_SYNC -> navigate(
            AuthenticationNavigationTransition.SESSION_SETUP_TO_INITIAL_SYNC,
            InitialSyncRoute(sessionId),
            WireBackStackMode.CLEAR_WHOLE,
        )
    }

    fun homeRequirement(
        requirement: HomeRequirement,
        currentSessionId: WireSessionId?,
    ): Boolean = when (requirement) {
        is HomeRequirement.RegisterDevice -> navigate(
            AuthenticationNavigationTransition.AUTH_TO_REGISTER_DEVICE,
            RegisterDeviceRoute(WireSessionId(requirement.userId.value, requirement.userId.domain)),
            WireBackStackMode.CLEAR_WHOLE,
        )

        HomeRequirement.CreateAccountUsername -> navigate(
            AuthenticationNavigationTransition.AUTH_TO_USERNAME,
            CreateAccountUsernameRoute.start(
                checkNotNull(currentSessionId) {
                    "$requirement requires the active session"
                }
            ),
            WireBackStackMode.CLEAR_WHOLE,
        )

        HomeRequirement.InitialSync -> navigate(
            AuthenticationNavigationTransition.AUTH_TO_INITIAL_SYNC,
            InitialSyncRoute(
                checkNotNull(currentSessionId) {
                    "InitialSync requires an active session"
                }
            ),
            WireBackStackMode.CLEAR_WHOLE,
        )
    }

    fun registerDeviceToE2EI(sessionId: WireSessionId, flowId: String): Boolean = navigate(
        AuthenticationNavigationTransition.REGISTER_DEVICE_TO_E2EI,
        E2EIEnrollmentRoute(sessionId = sessionId, flowId = flowId),
        WireBackStackMode.CLEAR_WHOLE,
    )

    fun removeDeviceToE2EI(sessionId: WireSessionId, flowId: String): Boolean = navigate(
        AuthenticationNavigationTransition.REMOVE_DEVICE_TO_E2EI,
        E2EIEnrollmentRoute(sessionId = sessionId, flowId = flowId),
        WireBackStackMode.CLEAR_WHOLE,
    )

    fun registerDeviceToRemoveDevice(sessionId: WireSessionId, flowId: String): Boolean = navigate(
        AuthenticationNavigationTransition.REGISTER_DEVICE_TO_REMOVE_DEVICE,
        RemoveDeviceRoute(sessionId = sessionId, flowId = flowId),
    )

    private fun currentAuthenticationFlowId(sessionId: WireSessionId): String =
        runtime.navigator.currentRoute?.flowId ?: sessionId.authenticationSessionFlowId()

    fun backOrElse(onRoot: () -> Unit) {
        val transitionId = WireNavigationDiagnostics.nextTransitionId()
        val popped = runtime.navigator.goBack()
        WireNavigationDiagnostics.auth(
            transitionId = transitionId,
            event = AuthenticationNavigationTransition.BACK.name,
            outcome = if (popped) "popped" else "root-fallback",
        )
        if (!popped) onRoot()
    }

    fun replaceCurrent(
        transition: AuthenticationNavigationTransition,
        route: WireRoute,
    ) {
        val transitionId = WireNavigationDiagnostics.nextTransitionId()
        val current = runtime.navigator.routes
        check(current.isNotEmpty()) {
            "Cannot replace an authentication route on an empty Navigation 3 stack"
        }
        WireNavigationDiagnostics.auth(
            transitionId = transitionId,
            event = transition.name,
            outcome = "replace-current:${route.routeId}",
        )
        runtime.navigator.replaceBackStack(current.dropLast(1) + route)
    }

    val canNavigateBack: Boolean
        get() = runtime.navigator.routes.size > 1
}

internal fun List<WireRoute>.hasActiveAuthenticationFlow(): Boolean =
    lastOrNull() is com.wire.navigation.AuthenticationRoute

internal class AuthenticationTransitionLedger {
    private val appliedEventIds = mutableSetOf<String>()

    @Synchronized
    fun executeOnce(
        eventId: String,
        action: () -> Boolean,
    ): Outcome {
        require(eventId.isNotBlank()) { "Authentication transition event id cannot be blank" }
        if (eventId in appliedEventIds) return Outcome.ALREADY_APPLIED
        return if (action()) {
            appliedEventIds += eventId
            Outcome.APPLIED
        } else {
            Outcome.REJECTED
        }
    }

    enum class Outcome {
        APPLIED,
        REJECTED,
        ALREADY_APPLIED,
    }
}
