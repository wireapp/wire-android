package com.wire.android.ui.authentication.create.common

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.feature.authentication.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountFlowPolicyTest {
    @Test
    fun `personal flow does not require team-specific presentation`() {
        val policy = CreateAccountRouteFlowType.PERSONAL.createAccountFlowPolicy()

        assertFalse(policy.isTeam)
        assertEquals(R.string.create_personal_account_title, policy.titleResId)
        assertEquals(R.string.create_personal_account_email_text, policy.emailSubtitleResId)
        assertEquals(R.string.create_personal_account_text, policy.overview.contentTextResId)
        assertEquals(R.drawable.ic_create_personal_account, policy.overview.contentIconResId)
    }

    @Test
    fun `team flow requires team-specific presentation`() {
        val policy = CreateAccountRouteFlowType.TEAM.createAccountFlowPolicy()

        assertTrue(policy.isTeam)
        assertEquals(R.string.create_team_title, policy.titleResId)
        assertEquals(R.string.create_team_email_text, policy.emailSubtitleResId)
        assertEquals(R.string.create_team_text, policy.overview.contentTextResId)
        assertEquals(R.drawable.ic_create_team, policy.overview.contentIconResId)
    }
}
