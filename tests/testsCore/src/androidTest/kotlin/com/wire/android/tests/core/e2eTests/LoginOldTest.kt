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
@file:Suppress("ArgumentListWrapping")

package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginOldTest {

    @TestCaseId("TC-4435", "TC-4438")
    @Category("loginOld", "login", "regression", "RC", "stale")
    @Ignore("Stale: old login-flow deep link and Welcome Page login entry are no longer exposed by the current app.")
    @Test
    fun givenOldLoginFlow_whenLoggingInWithEmail_thenLegacyFlowIsStale() = Unit

    @TestCaseId("TC-4442")
    @Category("loginOld", "regression", "RC", "stale")
    @Ignore(
        "Stale: wrong-email validation belongs to the removed old login flow; current negative login coverage lives in LoginNegativeTest."
    )
    @Test
    fun givenOldLoginFlow_whenUsingWrongEmailCredentials_thenLegacyFlowIsStale() = Unit

    @TestCaseId("TC-4436")
    @Category("loginOld", "regression", "RC", "stale")
    @Ignore(
        "Stale: wrong-password validation belongs to the removed old login flow; current negative login coverage lives in LoginNegativeTest."
    )
    @Test
    fun givenOldLoginFlow_whenUsingWrongPassword_thenLegacyFlowIsStale() = Unit

    @TestCaseId("TC-4440")
    @Category("loginOld", "regression", "RC", "stale")
    @Ignore("Stale: username login is part of the removed old login flow; current login UI asks for email or SSO code.")
    @Test
    fun givenOldLoginFlow_whenLoggingInWithUsername_thenLegacyFlowIsStale() = Unit

    @TestCaseId("TC-4441")
    @Category("loginOld", "sessionExpiration", "regression", "RC", "stale")
    @Ignore("Stale/blocked: combines removed old login flow with missing Kotlin helper for removing all registered OTR clients.")
    @Test
    fun givenOldLoginFlow_whenDeviceWasRemoved_thenLegacyReloginFlowIsStale() = Unit

    @TestCaseId("TC-4443")
    @Category("loginOld", "regression", "RC", "stale")
    @Ignore("Stale: forgot-password link belongs to the removed old login flow; current reset-password coverage is tracked under Settings.")
    @Test
    fun givenOldLoginFlow_whenTappingForgotPassword_thenLegacyFlowIsStale() = Unit

    @TestCaseId("TC-8711")
    @Category("loginOld", "SSO", "regression", "RC", "stale")
    @Ignore("Stale/blocked: old SSO login tab is removed and Okta setup is already blocked in the current SSO tests.")
    @Test
    fun givenOldLoginFlow_whenLoggingInWithSsoCode_thenLegacyFlowIsStale() = Unit
}
