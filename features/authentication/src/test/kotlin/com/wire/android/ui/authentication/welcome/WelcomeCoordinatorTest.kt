/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeCoordinatorTest {
    @Test
    fun `team creation prioritizes proxy restriction`() {
        val decision = welcomeTeamDecision(WelcomePolicy("links", true, true, "https://manage", "https://register"))
        assertEquals(WelcomeDecision.Dialog(WelcomeDialog.TeamBlockedByProxy("https://manage")), decision)
    }

    @Test
    fun `personal creation selects the configured registration flow`() {
        val hosted = welcomePersonalDecision(WelcomePolicy("links", false, true, "manage", "register"))
        val legacy = welcomePersonalDecision(WelcomePolicy("links", false, false, "manage", "register"))
        assertEquals(WelcomeDecision.Action(WelcomeAction.CreateAccountData("links")), hosted)
        assertEquals(WelcomeDecision.Action(WelcomeAction.CreatePersonal("links")), legacy)
    }

    @Test
    fun `personal proxy branch shows a dialog while registration uses the backlink URL`() {
        val blocked = welcomePersonalDecision(WelcomePolicy("links", true, true, "manage", "register"))
        val registration = welcomeTeamDecision(WelcomePolicy("links", false, true, "manage", "register"))
        assertEquals(WelcomeDecision.Dialog(WelcomeDialog.PersonalBlockedByProxy), blocked)
        assertEquals(WelcomeDecision.Action(WelcomeAction.OpenUrl("register")), registration)
    }
}
