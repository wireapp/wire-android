package com.wire.android.ui.authentication.create.common

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountFlowPolicyTest {
    @Test
    fun `personal flow does not require team-specific presentation`() {
        val policy = CreateAccountRouteFlowType.PERSONAL.createAccountFlowPolicy()

        assertFalse(policy.isTeam)
        assertTrue(policy.usesPersonalOverview)
    }

    @Test
    fun `team flow requires team-specific presentation`() {
        val policy = CreateAccountRouteFlowType.TEAM.createAccountFlowPolicy()

        assertTrue(policy.isTeam)
        assertFalse(policy.usesPersonalOverview)
    }
}
