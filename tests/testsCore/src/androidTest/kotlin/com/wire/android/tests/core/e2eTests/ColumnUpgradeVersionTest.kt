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
class ColumnUpgradeVersionTest : BaseUiTest() {

    @Ignore(
        "Blocked: column upgrade parity requires old column APK preinstall, new column APK path, " +
            "column backend deeplink/login, email 2FA, MLS setup, and image/unread remote-message helpers."
    )
    @TestCaseId("TC-4853")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnTeamUserWithHistory_whenUpgrading_thenHistoryUnreadAssetsAndSendReceiveStillWork() = Unit

    @Ignore(
        "Blocked: column upgrade assets parity requires old/new column APK orchestration, column login/2FA, " +
            "and stable remote image/video/text-file send plus post-upgrade asset assertions."
    )
    @TestCaseId("TC-4854")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnTeamUserWithAssets_whenUpgrading_thenImageVideoAndTextFileRemainVisible() = Unit

    @Ignore(
        "Blocked: column left-group upgrade parity requires old/new column APK orchestration, column login/2FA, " +
            "stable group-details leave selectors, and post-leave remote-send negative assertions."
    )
    @TestCaseId("TC-4855")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnUserLeftGroupBeforeUpgrade_whenUpgrading_thenOldHistoryRemainsAndNewMessagesStayHidden() = Unit

    @Ignore(
        "Blocked: column clear-content upgrade parity requires old/new column APK orchestration, column login/2FA, " +
            "group-details clear-content selectors, and negative message/image assertions after upgrade."
    )
    @TestCaseId("TC-4856")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnUserClearedGroupBeforeUpgrade_whenUpgrading_thenClearedHistoryStaysCleared() = Unit

    @Ignore(
        "Blocked: column deleted-user upgrade parity requires old/new column APK orchestration, column login/2FA, " +
            "team-removal propagation, and deleted-user conversation-list status selectors."
    )
    @TestCaseId("TC-4857")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnUserHadConversationWithDeletedUser_whenUpgrading_thenDeletedStatusIsPreserved() = Unit

    @Ignore(
        "Blocked: column deleted-group upgrade parity requires old/new column APK orchestration, column login/2FA, " +
            "backend delete-conversation setup, sync wait, and post-upgrade absence assertions."
    )
    @TestCaseId("TC-4858")
    @Category("upgrade", "column", "blocked")
    @Test
    fun givenColumnGroupWasDeletedBeforeUpgrade_whenUpgrading_thenGroupRemainsAbsent() = Unit
}
