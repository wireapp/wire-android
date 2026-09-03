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

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AuthenticationTransitionLedgerTest {

    @Test
    fun givenEmptyLoginIsTheOnlyRoute_whenActivityRequestsLogin_thenItIsCurrentRoot() {
        val route = NewLoginRoute.start()

        assertEquals(true, listOf(route).hasActiveAuthenticationFlow())
    }

    @Test
    fun givenLoginContainsIntentArguments_whenActivityRequestsLogin_thenItRemainsCurrentRoot() {
        val route = NewLoginRoute.start(
            AuthenticationLoginArguments(
                userHandle = AuthenticationPrefilledUserIdentifier("wire-user"),
            )
        )

        assertEquals(true, listOf(route).hasActiveAuthenticationFlow())
    }

    @Test
    fun givenPasswordIsCurrentInLoginFlow_whenActivityRequestsLogin_thenFlowRemainsActive() {
        val login = NewLoginRoute.start()

        assertEquals(true, listOf(login, NewLoginPasswordRoute(login.args, login.flowId)).hasActiveAuthenticationFlow())
    }

    @Test
    fun givenChooserPrecedesLogin_whenActivityRequestsLoginAgain_thenFlowRemainsActive() {
        val login = NewLoginRoute.start()

        assertEquals(
            true,
            listOf(WelcomeChooserRoute(), login).hasActiveAuthenticationFlow(),
        )
    }

    @Test
    fun givenEventWasApplied_whenSameEventRunsAgain_thenStackActionRunsOnlyOnce() {
        val ledger = AuthenticationTransitionLedger()
        var calls = 0

        val first = ledger.executeOnce("entry:terminal") {
            calls++
            true
        }
        val duplicate = ledger.executeOnce("entry:terminal") {
            calls++
            true
        }

        assertEquals(AuthenticationTransitionLedger.Outcome.APPLIED, first)
        assertEquals(AuthenticationTransitionLedger.Outcome.ALREADY_APPLIED, duplicate)
        assertEquals(1, calls)
    }

    @Test
    fun givenEventWasRejected_whenSameEventRunsAgain_thenItCanBeRetried() {
        val ledger = AuthenticationTransitionLedger()
        var accepted = false

        val rejected = ledger.executeOnce("entry:terminal") { accepted }
        accepted = true
        val retried = ledger.executeOnce("entry:terminal") { accepted }

        assertEquals(AuthenticationTransitionLedger.Outcome.REJECTED, rejected)
        assertEquals(AuthenticationTransitionLedger.Outcome.APPLIED, retried)
    }

    @Test
    fun givenBlankEventId_whenExecuting_thenContractFailsFast() {
        val ledger = AuthenticationTransitionLedger()

        assertThrows(IllegalArgumentException::class.java) {
            ledger.executeOnce("") { true }
        }
    }

    @Test
    fun givenConcurrentCollectors_whenTheyApplySameTerminalEvent_thenOnlyOneMutatesNavigation() {
        val ledger = AuthenticationTransitionLedger()
        val actionCalls = AtomicInteger()
        val executor = Executors.newFixedThreadPool(8)

        val outcomes = try {
            executor.invokeAll(
                List(64) {
                    Callable {
                        ledger.executeOnce("entry:concurrent-terminal") {
                            actionCalls.incrementAndGet()
                            true
                        }
                    }
                }
            ).map { it.get() }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(1, actionCalls.get())
        assertEquals(1, outcomes.count { it == AuthenticationTransitionLedger.Outcome.APPLIED })
        assertEquals(
            63,
            outcomes.count { it == AuthenticationTransitionLedger.Outcome.ALREADY_APPLIED },
        )
    }
}
