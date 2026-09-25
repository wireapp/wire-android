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
package com.wire.android.util

import android.app.Application
import android.net.Uri
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SSOFailureRedirectIntegrationTest {

    @Test
    fun `generated SSO failure redirect is parsed by the app`() = runTest {
        val redirect = generatedFailureRedirect().replace("\$label", ERROR_LABEL)

        val result = deepLinkProcessor()(Uri.parse(redirect))

        assertEquals(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Forbidden), result)
    }

    @Test
    fun `legacy SSO failure redirect is still parsed by the app`() = runTest {
        val redirect = generatedFailureRedirect()
            .replace("?errorCode=", "?error=")
            .replace("\$label", ERROR_LABEL)

        val result = deepLinkProcessor()(Uri.parse(redirect))

        assertEquals(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Forbidden), result)
    }

    /**
     * SSOUtil is internal to Kalium, so invoke the production JVM generator without duplicating
     * its callback template in this app-side integration test.
     */
    private fun generatedFailureRedirect(): String {
        val ssoUtilClass = Class.forName("com.wire.kalium.logic.data.sso.SSOUtil")
        val instance = ssoUtilClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
        val generator = ssoUtilClass.declaredMethods.single {
            it.name.startsWith("generateErrorRedirect") && it.parameterCount == 0
        }.apply { isAccessible = true }
        return generator.invoke(instance) as String
    }

    private fun deepLinkProcessor(): DeepLinkProcessor {
        val currentSession = mockk<CurrentSessionUseCase>()
        coEvery { currentSession() } returns CurrentSessionResult.Failure.SessionNotFound
        return DeepLinkProcessor(
            accountSwitch = mockk<AccountSwitchUseCase>(relaxed = true),
            currentSession = currentSession,
            coreLogic = mockk<CoreLogic>(relaxed = true),
        )
    }

    private companion object {
        const val ERROR_LABEL = "forbidden"
    }
}
