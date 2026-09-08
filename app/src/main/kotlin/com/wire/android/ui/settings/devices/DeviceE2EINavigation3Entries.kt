/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.settings.devices

import androidx.compose.runtime.Composable
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.AuthenticationNavigationTransition
import com.wire.android.navigation.routes.auth.RegisterDeviceCompletion
import com.wire.android.navigation.runtime.sessionCancellationSwitchAccountActions
import com.wire.android.ui.authentication.clearSessionViewModel
import com.wire.android.ui.authentication.registerDeviceViewModel
import com.wire.android.ui.authentication.removeDeviceViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRoute
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRouteScreen
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRouteScreen
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRouteScreen
import com.wire.android.ui.e2EIEnrollmentViewModel
import com.wire.android.ui.home.settings.deviceDetailsViewModel
import com.wire.android.ui.home.settings.e2eiCertificateDetailsViewModel
import com.wire.android.ui.home.settings.selfDevicesViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsPayload
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsRoute
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsRouteScreen
import com.wire.android.ui.settings.devices.e2ei.E2EICertificateDetails
import com.wire.android.ui.settings.devices.e2ei.toNavigationPayload
import com.wire.android.ui.settings.devices.e2ei.toViewModelArgs
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner

internal enum class SessionSetupDestination {
    HOME,
    INITIAL_SYNC,
}

/**
 * Operations whose destinations are owned by another migration batch.
 *
 * Account-switch callbacks deliberately implement the existing domain interface, so cancelling a
 * session-backed authentication flow keeps the same logout/switch ordering without importing a
 * generated destination.
 */
internal interface DeviceE2EINavigation3Actions : SwitchAccountActions {
    /**
     * Used when a restored/deep-linked device route has no parent entry to pop to.
     */
    fun exitDeviceManagement()

    /** Finalizes the graph generation after navigation leaves a cancelled login session. */
    fun completeSessionBackedAuthenticationCancellation(sessionId: WireSessionId)
}

internal object DeviceE2EINavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> = emptyList()

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: DeviceE2EINavigation3Actions,
        authenticationRouter: AuthenticationNavigation3Router,
    ): List<WireEntryProviderInstaller> = listOf(
        deviceE2EINavigation3Entries(runtime, actions, authenticationRouter),
    )
}

internal fun deviceE2EINavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: DeviceE2EINavigation3Actions,
    authenticationRouter: AuthenticationNavigation3Router,
): WireEntryProviderInstaller = {
    wireEntry<RegisterDeviceRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        RegisterDeviceNavigation3Entry(route, actions, authenticationRouter)
    }
    wireEntry<RemoveDeviceRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        RemoveDeviceNavigation3Entry(route, actions, authenticationRouter)
    }
    wireEntry<E2EIEnrollmentRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        E2EIEnrollmentNavigation3Entry(route, actions, authenticationRouter)
    }
    wireEntry<SelfDevicesRoute> { route ->
        SelfDevicesNavigation3Entry(runtime, route, actions)
    }
    wireEntry<DeviceDetailsRoute>(presentation = WireEntryPresentation.Slide) { route ->
        DeviceDetailsNavigation3Entry(runtime, route, actions)
    }
    wireEntry<E2eiCertificateDetailsRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        E2eiCertificateDetailsNavigation3Entry(runtime, route, actions)
    }
}

@Composable
private fun RegisterDeviceNavigation3Entry(
    route: RegisterDeviceRoute,
    actions: DeviceE2EINavigation3Actions,
    authenticationRouter: AuthenticationNavigation3Router,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    RegisterDeviceRouteScreen(
        viewModel = registerDeviceViewModel(flowOwner),
        clearSessionViewModel = clearSessionViewModel(flowOwner),
        switchAccountActions = sessionCancellationSwitchAccountActions(
            delegate = actions,
            sessionId = route.sessionId,
            onNavigationCompleted = actions::completeSessionBackedAuthenticationCancellation,
        ),
        onE2EIRequired = { userId ->
            authenticationRouter.completeRegisterDevice(
                eventId = route.registerDeviceTerminalEventId(),
                routeSessionId = route.sessionId,
                flowId = route.flowId,
                completion = RegisterDeviceCompletion.E2EIEnrollment(
                    userId?.let { WireSessionId(it.value, it.domain) } ?: route.sessionId,
                ),
            )
        },
        onHomeRequired = {
            authenticationRouter.completeRegisterDevice(
                eventId = route.registerDeviceTerminalEventId(),
                routeSessionId = route.sessionId,
                flowId = route.flowId,
                completion = RegisterDeviceCompletion.Home,
            )
        },
        onInitialSyncRequired = {
            authenticationRouter.completeRegisterDevice(
                eventId = route.registerDeviceTerminalEventId(),
                routeSessionId = route.sessionId,
                flowId = route.flowId,
                completion = RegisterDeviceCompletion.InitialSync,
            )
        },
        onRemoveDeviceRequired = {
            authenticationRouter.completeRegisterDevice(
                eventId = route.registerDeviceTerminalEventId(),
                routeSessionId = route.sessionId,
                flowId = route.flowId,
                completion = RegisterDeviceCompletion.RemoveDevice,
            )
        },
    )
}

private fun RegisterDeviceRoute.registerDeviceTerminalEventId(): String =
    "${entryId.value}:register-device-terminal"

@Composable
private fun RemoveDeviceNavigation3Entry(
    route: RemoveDeviceRoute,
    actions: DeviceE2EINavigation3Actions,
    authenticationRouter: AuthenticationNavigation3Router,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    RemoveDeviceRouteScreen(
        viewModel = removeDeviceViewModel(flowOwner),
        clearSessionViewModel = clearSessionViewModel(flowOwner),
        switchAccountActions = sessionCancellationSwitchAccountActions(
            delegate = actions,
            sessionId = route.sessionId,
            onNavigationCompleted = actions::completeSessionBackedAuthenticationCancellation,
        ),
        onE2EIRequired = {
            authenticationRouter.removeDeviceToE2EI(route.sessionId, route.flowId)
        },
        onHomeRequired = {
            authenticationRouter.completeSessionSetup(route.sessionId, SessionSetupDestination.HOME)
        },
        onInitialSyncRequired = {
            authenticationRouter.completeSessionSetup(route.sessionId, SessionSetupDestination.INITIAL_SYNC)
        },
    )
}

@Composable
private fun E2EIEnrollmentNavigation3Entry(
    route: E2EIEnrollmentRoute,
    actions: DeviceE2EINavigation3Actions,
    authenticationRouter: AuthenticationNavigation3Router,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    E2EIEnrollmentRouteScreen(
        viewModel = e2EIEnrollmentViewModel(flowOwner),
        clearSessionViewModel = clearSessionViewModel(flowOwner),
        switchAccountActions = sessionCancellationSwitchAccountActions(
            delegate = actions,
            sessionId = route.sessionId,
            onNavigationCompleted = actions::completeSessionBackedAuthenticationCancellation,
        ),
        onInitialSyncRequired = {
            authenticationRouter.completeSessionSetup(route.sessionId, SessionSetupDestination.INITIAL_SYNC)
        },
        onOpenCertificateDetails = { certificate ->
            authenticationRouter.navigate(
                AuthenticationNavigationTransition.E2EI_TO_CERTIFICATE_DETAILS,
                E2eiCertificateDetailsRoute(
                    sessionId = route.sessionId,
                    details = E2eiCertificateDetailsPayload.DuringLogin(certificate),
                )
            )
        },
    )
}

@Composable
private fun SelfDevicesNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: SelfDevicesRoute,
    actions: DeviceE2EINavigation3Actions,
) {
    val viewModel = selfDevicesViewModel()
    SelfDevicesRouteScreen(
        viewModel = viewModel,
        onNavigateBack = runtime.backOrExit(actions),
        onDeviceClick = { device ->
            runtime.openDeviceDetails(route, viewModel.currentAccountId, device)
        },
    )
}

@Composable
private fun DeviceDetailsNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: DeviceDetailsRoute,
    actions: DeviceE2EINavigation3Actions,
) {
    DeviceDetailsRouteScreen(
        viewModel = deviceDetailsViewModel(route.toViewModelArgs()),
        onNavigateBack = runtime.backOrExit(actions),
        onOpenCertificateDetails = { identity ->
            runtime.navigator.navigate(
                WireNavigationCommand(
                    E2eiCertificateDetailsRoute(
                        sessionId = route.sessionId,
                        details = E2EICertificateDetails
                            .AfterLoginCertificateDetails(identity)
                            .toNavigationPayload(),
                    )
                )
            )
        },
    )
}

@Composable
private fun E2eiCertificateDetailsNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: E2eiCertificateDetailsRoute,
    actions: DeviceE2EINavigation3Actions,
) {
    E2eiCertificateDetailsRouteScreen(
        viewModel = e2eiCertificateDetailsViewModel(route.toViewModelArgs()),
        onNavigateBack = runtime.backOrExit(actions),
    )
}

private fun WireNavigation3Runtime.openDeviceDetails(
    source: SelfDevicesRoute,
    userId: UserId,
    device: Device,
) {
    navigator.navigate(
        WireNavigationCommand(
            DeviceDetailsRoute(
                sessionId = source.sessionId,
                targetUserId = DeviceTargetUserId(userId.value, userId.domain),
                clientId = device.clientId.value,
            )
        )
    )
}

private fun WireNavigation3Runtime.backOrExit(
    actions: DeviceE2EINavigation3Actions,
): () -> Unit = {
    if (!navigator.goBack()) actions.exitDeviceManagement()
}
