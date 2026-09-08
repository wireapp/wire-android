/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui

import android.content.Context
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.runtime.startup.toWireSessionId
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.home.conversations.ConversationRouteId
import com.wire.android.ui.settings.devices.SelfDevicesRoute
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsPayload
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsRoute
import com.wire.android.ui.userprofile.self.SelfUserProfileRoute
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute

internal data class WireActivityDialogActionDependencies(
    val context: Context,
    val viewModel: WireActivityViewModel,
    val updateApp: () -> Unit,
    val startTeamAppLock: () -> Unit,
)

@Suppress("LongMethod")
internal fun navigation3DialogActions(
    runtime: WireNavigation3Runtime,
    switchAccountActions: SwitchAccountActions,
    dependencies: WireActivityDialogActionDependencies,
): WireActivityDialogActions {
    val sessionId = {
        dependencies.viewModel.globalAppState.currentUserId?.toWireSessionId()
    }
    val navigate: (WireRoute, WireBackStackMode) -> Unit = { route, mode ->
        runtime.navigator.navigate(WireNavigationCommand(route, mode))
    }
    return WireActivityDialogActions(
        updateApp = dependencies.updateApp,
        openLoginIfEmptyWelcomeStart = {
            if (runtime.navigator.routes.firstOrNull() is NewWelcomeEmptyStartRoute) {
                navigate(NewLoginRoute.start(), WireBackStackMode.CLEAR_WHOLE)
            }
        },
        openSelfProfile = {
            sessionId()?.let { navigate(SelfUserProfileRoute(it), WireBackStackMode.NONE) }
        },
        openSelfDevices = {
            sessionId()?.let { navigate(SelfDevicesRoute(it), WireBackStackMode.NONE) }
        },
        switchAccountAndOpenSelfDevices = { userId ->
            dependencies.viewModel.switchAccount(
                userId = userId,
                actions = switchAccountActions,
                onComplete = {
                    sessionId()?.let {
                        navigate(SelfDevicesRoute(it), WireBackStackMode.NONE)
                    }
                },
            )
        },
        openE2EICertificateDetails = { certificate ->
            sessionId()?.let {
                navigate(
                    E2eiCertificateDetailsRoute(
                        sessionId = it,
                        details = E2eiCertificateDetailsPayload.DuringLogin(certificate),
                    ),
                    WireBackStackMode.NONE,
                )
            }
        },
        openJoinedConversation = { conversationId ->
            sessionId()?.let {
                navigate(
                    ConversationRoute(
                        sessionId = it,
                        conversationId = ConversationRouteId(
                            conversationId.value,
                            conversationId.domain,
                        ),
                    ),
                    WireBackStackMode.CLEAR_TILL_START,
                )
            }
        },
        startTeamAppLock = dependencies.startTeamAppLock,
        hardLogout = {
            dependencies.viewModel.doHardLogout(
                clearUserData = { UserDataStore(dependencies.context, it) },
                switchAccountActions = switchAccountActions,
            )
        },
        recoverFromLoggedOutSession = {
            dependencies.viewModel.tryToSwitchAccount(switchAccountActions)
        },
    )
}
