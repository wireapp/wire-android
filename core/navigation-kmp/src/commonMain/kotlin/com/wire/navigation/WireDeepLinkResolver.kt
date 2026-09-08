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

import kotlinx.serialization.Serializable

/**
 * Platform-independent input to Wire's typed deep-link resolvers.
 *
 * Platform code remains responsible for deciding whether an Android Intent, iOS URL, or another
 * external event is a deep link. Keeping the original value here allows a deferred link to be
 * replayed after authentication or another prerequisite without converting it to Android types.
 */
@Serializable
data class WireDeepLinkRequest(val value: String)

/**
 * Terminal result of resolving a deep link.
 *
 * There is deliberately no "use the default route" outcome. Callers must handle unsupported and
 * invalid links explicitly instead of silently opening an unrelated screen.
 */
sealed interface WireDeepLinkResolution {

    /**
     * A complete, typed stack to install in the Navigation 3 runtime.
     */
    data class Resolved(val routes: List<WireRoute>) : WireDeepLinkResolution {
        init {
            require(routes.isNotEmpty()) { "A resolved deep-link stack cannot be empty" }
            require(routes.map(WireRoute::entryId).distinct().size == routes.size) {
                "A resolved deep-link stack cannot contain duplicate entry ids"
            }
        }
    }

    /**
     * The link is known, but its destination requires an authenticated session.
     */
    data class AuthenticationRequired(
        val pendingRequest: WireDeepLinkRequest,
    ) : WireDeepLinkResolution

    /**
     * The link is known, but app-owned work must finish before routes can be produced.
     */
    data class Deferred(
        val pendingRequest: WireDeepLinkRequest,
        val reason: WireDeepLinkDeferredReason,
    ) : WireDeepLinkResolution

    /**
     * The input is structurally valid but no registered resolver owns it.
     */
    data object Unsupported : WireDeepLinkResolution

    /**
     * The input is empty or a resolver recognized it but rejected its structure or arguments.
     */
    data class Invalid(val reason: WireDeepLinkInvalidReason) : WireDeepLinkResolution
}

/**
 * Cross-platform categories only. Concrete account switching, SSO, and validation state stays in
 * the app-owned resolver that returns one of these categories.
 */
enum class WireDeepLinkDeferredReason {
    SESSION_TRANSITION,
    EXTERNAL_FLOW,
    BUSINESS_VALIDATION,
}

enum class WireDeepLinkInvalidReason {
    EMPTY_INPUT,
    MALFORMED,
    MISSING_REQUIRED_ARGUMENT,
    INVALID_ARGUMENT,
}

/**
 * A resolver either declines an input or claims it with a terminal result.
 *
 * Once claimed, even [WireDeepLinkResolution.Invalid] is terminal. This prevents a malformed link
 * for a high-priority route from falling through to a broader resolver.
 */
fun interface WireDeepLinkResolver {
    fun resolve(request: WireDeepLinkRequest): WireDeepLinkResolverResult
}

sealed interface WireDeepLinkResolverResult {
    data object NotMatched : WireDeepLinkResolverResult

    data class Matched(
        val resolution: WireDeepLinkResolution,
    ) : WireDeepLinkResolverResult
}

/**
 * Resolves links in registration order. Earlier resolvers have deterministic precedence.
 */
class WireDeepLinkResolverChain(
    private val resolvers: List<WireDeepLinkResolver>,
) {
    fun resolve(request: WireDeepLinkRequest): WireDeepLinkResolution =
        if (request.value.isBlank()) {
            WireDeepLinkResolution.Invalid(WireDeepLinkInvalidReason.EMPTY_INPUT)
        } else {
            resolvers.firstNotNullOfOrNull { resolver ->
                (resolver.resolve(request) as? WireDeepLinkResolverResult.Matched)?.resolution
            } ?: WireDeepLinkResolution.Unsupported
        }
}
