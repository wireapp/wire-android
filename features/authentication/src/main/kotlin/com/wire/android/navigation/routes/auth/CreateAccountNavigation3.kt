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

import com.wire.navigation.AuthBackgroundRoute
import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
enum class CreateAccountRouteFlowType {
    PERSONAL,
    TEAM,
}

@Serializable
data class CreateAccountRegistrationInfo(
    val email: String = "",
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    val teamName: String = "",
    val teamIcon: String = "default",
)

@Serializable
data class CreateAccountSelectorRoute(
    val customServerConfig: AuthenticationServerLinks? = null,
    val email: String? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_selector_screen"
    }
}

@Serializable
data class CreateAccountDataDetailRoute(
    val registrationInfo: CreateAccountRegistrationInfo = CreateAccountRegistrationInfo(),
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_data_detail_screen"
    }
}

@Serializable
data class CreateAccountVerificationCodeRoute(
    val registrationInfo: CreateAccountRegistrationInfo,
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_verification_code_screen"
    }
}

@Serializable
data class CreatePersonalAccountOverviewRoute(
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_personal_account_overview_screen"
    }
}

@Serializable
data class CreateTeamAccountOverviewRoute(
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_team_account_overview_screen"
    }
}

@Serializable
data class CreateAccountEmailRoute(
    val type: CreateAccountRouteFlowType,
    val registrationInfo: CreateAccountRegistrationInfo = CreateAccountRegistrationInfo(),
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_email_screen"
    }
}

@Serializable
data class CreateAccountDetailsRoute(
    val type: CreateAccountRouteFlowType,
    val registrationInfo: CreateAccountRegistrationInfo,
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_details_screen"
    }
}

@Serializable
data class CreateAccountCodeRoute(
    val type: CreateAccountRouteFlowType,
    val registrationInfo: CreateAccountRegistrationInfo,
    val customServerConfig: AuthenticationServerLinks? = null,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : AuthenticationRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_code_screen"
    }
}

@Serializable
data class CreateAccountSummaryRoute(
    val type: CreateAccountRouteFlowType,
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute, AuthenticationScreenRoute, AuthBackgroundRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_summary_screen"
    }
}

@Serializable
data class CreateAccountUsernameRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute, AuthenticationScreenRoute, AuthBackgroundRoute {
    override val routeId: String get() = ROUTE_ID

    init {
        require(flowId.isNotBlank()) { "A create-account flow id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/create_account_username_screen"

        fun start(sessionId: WireSessionId): CreateAccountUsernameRoute {
            val entryId = WireNavEntryId.random()
            return CreateAccountUsernameRoute(
                sessionId = sessionId,
                flowId = entryId.value,
                entryId = entryId,
            )
        }
    }
}
