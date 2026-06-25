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
class SSOProvisioningTest {

    @TestCaseId("TC-4550")
    @Category("SSO", "settings", "regression", "RC")
    @Ignore(
        "Blocked: Okta setup is currently blocked by local token/config issues before app login; needs SSO provisioning fixture parity."
    )
    @Test
    fun givenOktaSsoUser_whenOpeningAccountDetails_thenResetPasswordIsNotVisible() = Unit

    @TestCaseId("TC-4551")
    @Category("SSO", "settings", "regression", "RC")
    @Ignore("Blocked: needs Okta plus SCIM-managed user provisioning fixture parity before profile-name edit restrictions can be verified.")
    @Test
    fun givenScimManagedSsoUser_whenOpeningAccountDetails_thenProfileNameCannotBeEdited() = Unit
}
