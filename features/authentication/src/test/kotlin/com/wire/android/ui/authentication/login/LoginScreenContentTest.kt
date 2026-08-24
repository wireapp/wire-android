/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginScreenContentTest {
    @Test fun `SSO is initial tab only for result or auto login`() {
        assertEquals(LoginTabItem.EMAIL, initialLoginTab(false, false))
        assertEquals(LoginTabItem.SSO, initialLoginTab(true, false))
        assertEquals(LoginTabItem.SSO, initialLoginTab(false, true))
    }

    @Test fun `backend configuration keeps success screen visible`() {
        assertTrue(shouldShowBackendSetup(false, false))
        assertTrue(shouldShowBackendSetup(true, true))
        assertFalse(shouldShowBackendSetup(true, false))
    }
}
