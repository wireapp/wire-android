/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.navigation.routes.auth

import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewNavArgs
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryNavArgs
import com.wire.android.ui.registration.selector.CreateAccountSelectorNavArgs
import com.wire.navigation.WireSessionId

internal fun CreateAccountSelectorRoute.toLegacyNavArgs(): CreateAccountSelectorNavArgs =
    CreateAccountSelectorNavArgs(customServerConfig?.toLegacy(), email)

internal fun CreateAccountDataDetailRoute.toLegacyNavArgs(): CreateAccountDataNavArgs =
    CreateAccountDataNavArgs(registrationInfo.toLegacy(), customServerConfig?.toLegacy())

internal fun CreateAccountVerificationCodeRoute.toLegacyNavArgs(): CreateAccountDataNavArgs =
    CreateAccountDataNavArgs(registrationInfo.toLegacy(), customServerConfig?.toLegacy())

internal fun CreatePersonalAccountOverviewRoute.toLegacyNavArgs(): CreateAccountOverviewNavArgs =
    CreateAccountOverviewNavArgs(customServerConfig?.toLegacy())

internal fun CreateTeamAccountOverviewRoute.toLegacyNavArgs(): CreateAccountOverviewNavArgs =
    CreateAccountOverviewNavArgs(customServerConfig?.toLegacy())

internal fun CreateAccountEmailRoute.toLegacyNavArgs(): CreateAccountNavArgs =
    CreateAccountNavArgs(type.toLegacy(), registrationInfo.toLegacy(), customServerConfig?.toLegacy())

internal fun CreateAccountDetailsRoute.toLegacyNavArgs(): CreateAccountNavArgs =
    CreateAccountNavArgs(type.toLegacy(), registrationInfo.toLegacy(), customServerConfig?.toLegacy())

internal fun CreateAccountCodeRoute.toLegacyNavArgs(): CreateAccountNavArgs =
    CreateAccountNavArgs(type.toLegacy(), registrationInfo.toLegacy(), customServerConfig?.toLegacy())

internal fun CreateAccountSummaryRoute.toLegacyNavArgs(): CreateAccountSummaryNavArgs =
    CreateAccountSummaryNavArgs(type.toLegacy())

internal fun CreateAccountSummaryNavArgs.toSummaryRoute(
    flowId: String,
    sessionId: WireSessionId,
): CreateAccountSummaryRoute =
    CreateAccountSummaryRoute(type.toRoute(), sessionId, flowId)

internal fun CreateAccountDataNavArgs.toDataDetailRoute(flowId: String): CreateAccountDataDetailRoute =
    CreateAccountDataDetailRoute(
        registrationInfo = userRegistrationInfo.toRoute(),
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        flowId = flowId,
    )

internal fun CreateAccountDataNavArgs.toVerificationCodeRoute(flowId: String): CreateAccountVerificationCodeRoute =
    CreateAccountVerificationCodeRoute(
        registrationInfo = userRegistrationInfo.toRoute(),
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        flowId = flowId,
    )

internal fun CreateAccountNavArgs.toEmailRoute(flowId: String): CreateAccountEmailRoute =
    CreateAccountEmailRoute(
        type = flowType.toRoute(),
        registrationInfo = userRegistrationInfo.toRoute(),
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        flowId = flowId,
    )

internal fun CreateAccountNavArgs.toDetailsRoute(flowId: String): CreateAccountDetailsRoute =
    CreateAccountDetailsRoute(
        type = flowType.toRoute(),
        registrationInfo = userRegistrationInfo.toRoute(),
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        flowId = flowId,
    )

internal fun CreateAccountNavArgs.toCodeRoute(flowId: String): CreateAccountCodeRoute =
    CreateAccountCodeRoute(
        type = flowType.toRoute(),
        registrationInfo = userRegistrationInfo.toRoute(),
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        flowId = flowId,
    )

private fun CreateAccountFlowType.toRoute(): CreateAccountRouteFlowType =
    when (this) {
        CreateAccountFlowType.CreatePersonalAccount -> CreateAccountRouteFlowType.PERSONAL
        CreateAccountFlowType.CreateTeam -> CreateAccountRouteFlowType.TEAM
    }

private fun CreateAccountRouteFlowType.toLegacy(): CreateAccountFlowType =
    when (this) {
        CreateAccountRouteFlowType.PERSONAL -> CreateAccountFlowType.CreatePersonalAccount
        CreateAccountRouteFlowType.TEAM -> CreateAccountFlowType.CreateTeam
    }

private fun UserRegistrationInfo.toRoute(): CreateAccountRegistrationInfo =
    CreateAccountRegistrationInfo(email, name, firstName, lastName, password, teamName, teamIcon)

private fun CreateAccountRegistrationInfo.toLegacy(): UserRegistrationInfo =
    UserRegistrationInfo(email, name, firstName, lastName, password, teamName, teamIcon)
