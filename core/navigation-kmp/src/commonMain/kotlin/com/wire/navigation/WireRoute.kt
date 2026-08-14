/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Serializable account identity used to select a session-scoped Metro graph.
 */
@Serializable
data class WireSessionId(
    val value: String,
    val domain: String,
) {
    init {
        require(value.isNotBlank()) { "A session id value cannot be blank" }
        require(domain.isNotBlank()) { "A session id domain cannot be blank" }
    }
}

/** Stable flow identity for session-backed authentication started outside an existing auth flow. */
fun WireSessionId.authenticationSessionFlowId(): String =
    "authentication-session:$value@$domain"

/**
 * Platform-independent navigation contract.
 *
 * Concrete routes live next to the feature that owns them. They must be serializable and only
 * contain values available to all supported Kotlin Multiplatform targets.
 */
interface WireRoute : NavKey {
    /**
     * Stable identity of this concrete back-stack entry.
     *
     * Concrete serializable routes generate this once in their constructor. It is intentionally
     * independent from [routeId], so equal destinations can coexist without sharing entry state.
     */
    val entryId: WireNavEntryId

    /**
     * Stable identity of a destination, excluding its arguments.
     *
     * This replaces matching generated destination base-route strings. Two route instances with
     * different arguments use the same [routeId].
     */
    val routeId: String

    /**
     * Optional identity of a flow that owns this route.
     *
     * Routes with the same non-null value can be removed together when their nested flow is
     * completed. It will also become the natural owner key for flow-scoped ViewModels.
     */
    val flowId: String?
        get() = null
}

/**
 * Marks a route that belongs to the authentication experience for screen observation/analytics.
 *
 * Session-backed authentication screens implement this together with [SessionRoute]. The marker
 * deliberately says nothing about dependency ownership.
 */
interface AuthenticationScreenRoute : WireRoute

/**
 * Marks an authentication screen that does not require an authenticated session graph.
 */
interface AuthenticationRoute : AuthenticationScreenRoute

/**
 * Marks a route whose dependencies belong to a specific account session.
 *
 * This selects the session-specific dependency graph. It does not change ViewModel ownership;
 * session-shared ViewModels require an explicit [WireViewModelOwner.Session] at their call site.
 */
interface SessionRoute : WireRoute {
    val sessionId: WireSessionId
}
