/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.email

import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginEmailTerminalEffectsTest {
    @Test
    fun `not claimed domain preserves successful navigation`() {
        val state = LoginState.Success(initialSyncCompleted = true, isE2EIRequired = false, userId = "user")
        val claim: DomainClaimedByOrg = DomainClaimedByOrg.NotClaimed
        val effect = loginTerminalEffect(state, claim as? DomainClaimedByOrg.Claimed)
        assertEquals(LoginTerminalEffect.Success(true, false, "user"), effect)
    }
}
