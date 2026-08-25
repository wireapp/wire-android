/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.newauthentication.login.password

import com.wire.android.ui.authentication.login.LoginState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NewLoginPasswordCoordinatorTest {
    @Test
    fun `account creation requires all policy conditions`() {
        assertTrue(showCreateAccount(NewLoginPasswordPolicy(true, false, true)))
        assertFalse(showCreateAccount(NewLoginPasswordPolicy(true, true, true)))
    }

    @Test
    fun `cancellation is a terminal route effect`() {
        assertEquals(NewLoginPasswordTerminal.Canceled, newLoginPasswordTerminal(LoginState.Canceled))
    }
}
