/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.email

import com.wire.android.ui.authentication.login.LoginState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginEmailEffectsTest {
    @Test
    fun `claimed domain defers successful navigation`() {
        val state = LoginState.Success(initialSyncCompleted = true, isE2EIRequired = false, userId = "user")
        assertEquals(LoginTerminalEffect.ShowClaimedDomain("domain"), loginTerminalEffect(state, "domain"))
    }

    @Test
    fun `too many devices requests device removal`() {
        val state = LoginState.Error.TooManyDevicesError("user")
        assertEquals(LoginTerminalEffect.RemoveDevice("user"), loginTerminalEffect(state, null))
    }
}
