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
package com.wire.android.ui.newauthentication.login

import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.sso.SSOUrlConfig
import com.wire.android.util.newServerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginNavigatorTest {

    @Test
    fun `given enterprise unsupported action, when mapped, then return enterprise unsupported command`() {
        val action = NewLoginAction.EnterpriseLoginNotSupported(USER_IDENTIFIER)

        val result = action.toLoginNavigationCommand()

        assertEquals(LoginNavigationCommand.EnterpriseLoginNotSupported(USER_IDENTIFIER), result)
    }

    @Test
    fun `given email password action, when mapped, then return email password command`() {
        val loginPasswordPath = LoginPasswordPath(isCloudAccountCreationPossible = true)
        val action = NewLoginAction.EmailPassword(USER_IDENTIFIER, loginPasswordPath)

        val result = action.toLoginNavigationCommand()

        assertEquals(LoginNavigationCommand.EmailPassword(USER_IDENTIFIER, loginPasswordPath), result)
    }

    @Test
    fun `given custom config action, when mapped, then return custom config command`() {
        val customServerConfig = newServerConfig(1).links
        val action = NewLoginAction.CustomConfig(USER_IDENTIFIER, customServerConfig)

        val result = action.toLoginNavigationCommand()

        assertEquals(LoginNavigationCommand.CustomConfig(USER_IDENTIFIER, customServerConfig), result)
    }

    @Test
    fun `given sso action, when mapped, then return sso command`() {
        val config = SSOUrlConfig(USER_IDENTIFIER)
        val action = NewLoginAction.SSO(SSO_URL, config)

        val result = action.toLoginNavigationCommand()

        assertEquals(LoginNavigationCommand.SSO(SSO_URL, config), result)
    }

    @Test
    fun `given success action, when mapped, then return success command`() {
        val expectedResults = mapOf(
            NewLoginAction.Success.NextStep.E2EIEnrollment to LoginNavigationCommand.Success.NextStep.E2EIEnrollment,
            NewLoginAction.Success.NextStep.InitialSync to LoginNavigationCommand.Success.NextStep.InitialSync,
            NewLoginAction.Success.NextStep.TooManyDevices to LoginNavigationCommand.Success.NextStep.TooManyDevices,
            NewLoginAction.Success.NextStep.None to LoginNavigationCommand.Success.NextStep.None,
        )

        expectedResults.forEach { (actionNextStep, commandNextStep) ->
            val result = NewLoginAction.Success(actionNextStep).toLoginNavigationCommand()

            assertEquals(LoginNavigationCommand.Success(commandNextStep), result)
        }
    }

    private companion object {
        const val USER_IDENTIFIER = "user@wire.com"
        const val SSO_URL = "https://wire.com/sso"
    }
}
