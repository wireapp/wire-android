/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeCoordinatorTest {
    @Test
    fun `team creation prioritizes proxy restriction`() {
        val decision = welcomeTeamDecision(WelcomePolicy("links", true, true, "https://teams"))
        assertEquals(WelcomeDecision.Dialog(WelcomeDialog.TeamBlockedByProxy("https://teams")), decision)
    }

    @Test
    fun `personal creation selects the configured registration flow`() {
        val hosted = welcomePersonalDecision(WelcomePolicy("links", false, true, "url"))
        val legacy = welcomePersonalDecision(WelcomePolicy("links", false, false, "url"))
        assertEquals(WelcomeDecision.Action(WelcomeAction.CreateAccountData("links")), hosted)
        assertEquals(WelcomeDecision.Action(WelcomeAction.CreatePersonal("links")), legacy)
    }
}
