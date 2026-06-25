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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeypackagesTest : BaseUiTest() {

    @Ignore(
        "Blocked: MLS keypackage depletion group-add parity needs claim/count helpers, column login/2FA, " +
            "stable create-group UI helpers, and current system-message copy assertions."
    )
    @TestCaseId("TC-8298", "TC-4776")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenInviteeHasNoKeypackages_whenAddingTwoUsersToMlsGroup_thenDepletedUserIsNotAdded() = Unit

    @Ignore(
        "Blocked: MLS group creation with only a depleted invitee needs claim/count helpers, column login/2FA, " +
            "stable create-group UI helpers, and current system-message copy assertions."
    )
    @TestCaseId("TC-8114")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenOnlyInviteeHasNoKeypackages_whenCreatingMlsGroup_thenConversationOpensWithoutInvitee() = Unit

    @Ignore(
        "Stale/blocked: source scenario is commented out because of keypackage claiming issues; activation needs " +
            "reliable keypackage claim/count helpers and create-group UI parity."
    )
    @TestCaseId("TC-8296", "TC-4774", "TC-8295", "TC-4773")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenUserHasLessThanFiftyKeypackages_whenOnlineAndAddedToGroup_thenOwnPackagesRenewAndInviteePackageIsConsumed() = Unit

    @Ignore(
        "Blocked: login renewal parity needs reliable keypackage claim/count helpers before and after login."
    )
    @TestCaseId("TC-8297", "TC-4775")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenUserHasLessThanFiftyKeypackages_whenLoggingIn_thenOwnPackagesRenewToOneHundred() = Unit

    @Ignore(
        "Blocked: creating an MLS group with depleted own keypackages needs claim/count helpers and stable create-group UI helpers."
    )
    @TestCaseId("TC-8299", "TC-4777")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenCurrentUserHasNoKeypackages_whenCreatingMlsGroup_thenGroupIsCreatedAndOwnCountRemainsZero() = Unit

    @Ignore(
        "Blocked: MLS 1:1 depleted-invitee parity needs keypackage claim/count helpers, profile start-conversation flow, " +
            "and unable-to-start-conversation alert selectors."
    )
    @TestCaseId("TC-8113")
    @Category("keypackages", "MLS", "regression", "blocked")
    @Test
    fun givenInviteeHasNoKeypackages_whenStartingOneOnOneConversation_thenUnableToStartConversationAlertIsShown() = Unit
}
