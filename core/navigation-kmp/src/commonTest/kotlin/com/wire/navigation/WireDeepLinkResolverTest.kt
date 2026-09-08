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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class WireDeepLinkResolverTest {

    @Test
    fun givenMultipleMatchingResolvers_whenResolving_thenFirstRegisteredResolverWins() {
        val chain = WireDeepLinkResolverChain(
            listOf(
                resolverWith(WireDeepLinkResolution.Resolved(listOf(route("specific")))),
                resolverWith(WireDeepLinkResolution.Resolved(listOf(route("broad")))),
            )
        )

        val result = chain.resolve(WireDeepLinkRequest("wire://conversation/id"))

        assertEquals(
            WireDeepLinkResolution.Resolved(listOf(route("specific"))),
            result,
        )
    }

    @Test
    fun givenEarlierResolverDoesNotMatch_whenResolving_thenNextResolverCanClaimLink() {
        val chain = WireDeepLinkResolverChain(
            listOf(
                WireDeepLinkResolver { WireDeepLinkResolverResult.NotMatched },
                resolverWith(WireDeepLinkResolution.Resolved(listOf(route("fallback")))),
            )
        )

        val result = chain.resolve(WireDeepLinkRequest("wire://conversation/id"))

        assertEquals(
            WireDeepLinkResolution.Resolved(listOf(route("fallback"))),
            result,
        )
    }

    @Test
    fun givenRecognizedMalformedLink_whenResolving_thenInvalidResultStopsFallback() {
        var fallbackInvocations = 0
        val chain = WireDeepLinkResolverChain(
            listOf(
                resolverWith(
                    WireDeepLinkResolution.Invalid(
                        WireDeepLinkInvalidReason.MISSING_REQUIRED_ARGUMENT
                    )
                ),
                WireDeepLinkResolver {
                    fallbackInvocations++
                    WireDeepLinkResolverResult.Matched(
                        WireDeepLinkResolution.Resolved(listOf(route("fallback")))
                    )
                },
            )
        )

        val result = chain.resolve(WireDeepLinkRequest("wire://conversation"))

        assertEquals(
            WireDeepLinkResolution.Invalid(WireDeepLinkInvalidReason.MISSING_REQUIRED_ARGUMENT),
            result,
        )
        assertEquals(0, fallbackInvocations)
    }

    @Test
    fun givenBlankInput_whenResolving_thenInputIsInvalidAndResolversAreNotInvoked() {
        var invocations = 0
        val chain = WireDeepLinkResolverChain(
            listOf(
                WireDeepLinkResolver {
                    invocations++
                    WireDeepLinkResolverResult.NotMatched
                }
            )
        )

        val result = chain.resolve(WireDeepLinkRequest("  "))

        assertEquals(
            WireDeepLinkResolution.Invalid(WireDeepLinkInvalidReason.EMPTY_INPUT),
            result,
        )
        assertEquals(0, invocations)
    }

    @Test
    fun givenNoResolverClaimsLink_whenResolving_thenResultIsExplicitlyUnsupported() {
        val chain = WireDeepLinkResolverChain(
            listOf(WireDeepLinkResolver { WireDeepLinkResolverResult.NotMatched })
        )

        val result = chain.resolve(WireDeepLinkRequest("wire://unknown"))

        assertIs<WireDeepLinkResolution.Unsupported>(result)
    }

    @Test
    fun givenKnownLinkWithoutSession_whenResolving_thenPendingRequestIsPreserved() {
        val request = WireDeepLinkRequest("wire://conversation/id")
        val chain = WireDeepLinkResolverChain(
            listOf(
                resolverWith(WireDeepLinkResolution.AuthenticationRequired(request))
            )
        )

        val result = chain.resolve(request)

        assertEquals(WireDeepLinkResolution.AuthenticationRequired(request), result)
    }

    @Test
    fun givenKnownLinkAwaitingBusinessWork_whenResolving_thenDeferredReasonIsExplicit() {
        val request = WireDeepLinkRequest("wire://conversation-join?code=x&key=y")
        val chain = WireDeepLinkResolverChain(
            listOf(
                resolverWith(
                    WireDeepLinkResolution.Deferred(
                        pendingRequest = request,
                        reason = WireDeepLinkDeferredReason.BUSINESS_VALIDATION,
                    )
                )
            )
        )

        val result = chain.resolve(request)

        assertEquals(
            WireDeepLinkResolution.Deferred(
                pendingRequest = request,
                reason = WireDeepLinkDeferredReason.BUSINESS_VALIDATION,
            ),
            result,
        )
    }

    @Test
    fun givenResolvedStackWithDuplicateEntryIds_whenCreatingResolution_thenItIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            WireDeepLinkResolution.Resolved(
                listOf(route("first", entryId = "duplicate"), route("second", entryId = "duplicate"))
            )
        }
    }

    private fun resolverWith(resolution: WireDeepLinkResolution) =
        WireDeepLinkResolver {
            WireDeepLinkResolverResult.Matched(resolution)
        }

    private fun route(
        routeId: String,
        entryId: String = "$routeId-entry",
    ) = TestRoute(routeId, WireNavEntryId(entryId))

    private data class TestRoute(
        override val routeId: String,
        override val entryId: WireNavEntryId,
    ) : WireRoute
}
