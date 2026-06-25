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
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeTeamTest {

    @TestCaseId("TC-4398", "TC-8630")
    @Category("largeTeams", "regression", "RC")
    @Ignore(
        "Blocked/stale: source uses fixed smoketest accounts and has disabled remote-device message parity until Kalium " +
            "test-service timeout is increased; Kotlin needs stable large-team fixtures and create/delete group UI helpers."
    )
    @Test
    fun givenLargeTeam_whenCreatingAndUsingGroupConversation_thenGroupLifecycleAndMessagingAreMappedOnly() = Unit
}
